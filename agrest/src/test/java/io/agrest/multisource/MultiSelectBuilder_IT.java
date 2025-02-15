package io.agrest.multisource;

import io.agrest.DataResponse;
import io.agrest.ResourceEntity;
import io.agrest.SelectBuilder;
import io.agrest.it.fixture.JerseyAndDerbyCase;
import io.agrest.it.fixture.cayenne.E22;
import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectId;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MultiSelectBuilder_IT extends JerseyAndDerbyCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiSelectBuilder_IT.class);

    @BeforeClass
    public static void startTestRuntime() {
        JerseyAndDerbyCase.startTestRuntime();
    }

    private static UriInfo mockUri(MultivaluedMap<String, String> params) {
        UriInfo mockUri = mock(UriInfo.class);
        when(mockUri.getQueryParameters()).thenReturn(params);
        return mockUri;
    }

    @Override
    protected Class<?>[] testEntities() {
        return new Class[]{E22.class};
    }

    // TODO: test attaching to subtree objects
    // TODO: test parent batches

    @Test
    public void testParallelThenMerge() {

        e22().insertColumns("id", "name")
                .values(5, "aa")
                .values(6, "bb")
                .values(4, "cc").exec();

        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.putSingle("sort", "id");

        SelectBuilder<E22> rootSelect = ag().select(E22.class).uri(mockUri(params));

        DataResponse<E22> response = new MultiSelectBuilder<>(rootSelect, agService(ExecutorService.class))
                .parallel(this::parallelFetcher, this::merge)
                .select(5, TimeUnit.SECONDS);

        Map<Integer, E22> rootsById = response.getIncludedObjects().stream()
                .collect(toMap(Cayenne::intPKForObject, o -> o));

        E22 e4 = rootsById.get(4);
        Assert.assertEquals("cc", e4.getName());
        assertEquals("_4", e4.getProp1());

        E22 e5 = rootsById.get(5);
        Assert.assertEquals("aa", e5.getName());
        assertEquals("_5", e5.getProp1());

        // this one is unmerged - no matches in the child fetcher
        E22 e6 = rootsById.get(6);
        Assert.assertEquals("bb", e6.getName());
        assertNull(e6.getProp1());
    }

    @Test
    public void testAfterParent_TwoSubfetchers() {

        e22().insertColumns("id", "name")
                .values(5, "aa")
                .values(6, "bb")
                .values(4, "cc").exec();

        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.putSingle("sort", "id");

        SelectBuilder<E22> rootSelect = ag().select(E22.class).uri(mockUri(params));

        DataResponse<E22> response = new MultiSelectBuilder<>(rootSelect, agService(ExecutorService.class))
                .afterParent(this::afterFetcher1, this::merge)
                .afterParent(this::afterFetcher2, this::merge)
                .select(5, TimeUnit.SECONDS);

        Map<Integer, E22> rootsById = response.getIncludedObjects().stream()
                .collect(toMap(Cayenne::intPKForObject, o -> o));

        E22 e4 = rootsById.get(4);
        Assert.assertEquals("cc", e4.getName());
        assertEquals("_cc", e4.getProp1());
        assertEquals("__cc", e4.getProp2());

        E22 e5 = rootsById.get(5);
        Assert.assertEquals("aa", e5.getName());
        assertEquals("_aa", e5.getProp1());
        assertEquals("__aa", e5.getProp2());

        E22 e6 = rootsById.get(6);
        Assert.assertEquals("bb", e6.getName());
        assertEquals("_bb", e6.getProp1());
        assertEquals("__bb", e6.getProp2());
    }

    public List<E22> parallelFetcher() {

        LOGGER.info("running parallel fetcher");

        return IntStream.range(0, 6).mapToObj(i -> {
            E22 proto = new E22();

            proto.setObjectId(new ObjectId("E22", "id", i));
            proto.setProp1("_" + i);

            return proto;
        }).collect(toList());
    }

    public List<E22> afterFetcher1(List<E22> parents) {

        LOGGER.info("running after-fetcher 2");

        return parents.stream().map(p -> {
            E22 proto = new E22();

            proto.setObjectId(p.getObjectId());
            proto.setProp1("_" + p.getName());

            return proto;
        }).collect(toList());
    }

    public List<E22> afterFetcher2(List<E22> parents, ResourceEntity<E22> entity) {

        assertNotNull(entity);

        LOGGER.info("running after-fetcher 2");

        return parents.stream().map(p -> {
            E22 proto = new E22();

            proto.setObjectId(p.getObjectId());
            proto.setProp2("__" + p.getName());

            return proto;
        }).collect(toList());
    }

    public void merge(List<E22> parents, List<E22> toMerge) {
        LOGGER.info("merging subfetcher results");

        Map<Integer, E22> parentsById = parents.stream().collect(toMap(Cayenne::intPKForObject, o -> o));

        toMerge.forEach(proto -> {
            E22 original = parentsById.get(Cayenne.intPKForObject(proto));
            if (original != null) {
                original.mergeTransient(proto);
            }
        });
    }

}
