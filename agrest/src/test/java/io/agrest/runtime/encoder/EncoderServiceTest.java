package io.agrest.runtime.encoder;

import com.fasterxml.jackson.core.JsonGenerator;
import io.agrest.DataResponse;
import io.agrest.ResourceEntity;
import io.agrest.SimpleObjectId;
import io.agrest.encoder.Encoder;
import io.agrest.encoder.EncoderFilter;
import io.agrest.encoder.Encoders;
import io.agrest.it.fixture.cayenne.E1;
import io.agrest.it.fixture.cayenne.E19;
import io.agrest.it.fixture.cayenne.E2;
import io.agrest.it.fixture.cayenne.E3;
import io.agrest.runtime.semantics.RelationshipMapper;
import io.agrest.unit.TestWithCayenneMapping;
import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class EncoderServiceTest extends TestWithCayenneMapping {

    private EncoderService encoderService;
    private List<EncoderFilter> filters;

    @Before
    public void before() {

        this.filters = new ArrayList<>();
        IAttributeEncoderFactory attributeEncoderFactory = new AttributeEncoderFactoryProvider(Collections.emptyMap()).get();
        IStringConverterFactory stringConverterFactory = mock(IStringConverterFactory.class);

        encoderService = new EncoderService(
                this.filters,
                attributeEncoderFactory,
                stringConverterFactory,
                new RelationshipMapper(),
                Collections.emptyMap());
    }

    @Test
    public void testGetRootEncoder_ExcludedAttributes() {
        // empty filter - must only include id
        ResourceEntity<E1> descriptor = getResourceEntity(E1.class);
        descriptor.includeId();

        E1 e1 = new E1();
        e1.setObjectId(new ObjectId("E1", E1.ID_PK_COLUMN, 777));
        e1.setName("XYZ");
        e1.setAge(30);
        e1.setDescription("test");

        assertEquals("{\"data\":[{\"id\":777}],\"total\":1}", toJson(e1, descriptor));
    }

    @Test
    public void testGetRootEncoder_ExcludedRelationshipAttributes() {
        ResourceEntity<E3> e3Descriptor = getResourceEntity(E3.class);
        e3Descriptor.includeId();

        appendAttribute(e3Descriptor, E3.NAME, String.class);

        ResourceEntity<E2> descriptor = getResourceEntity(E2.class);
        descriptor.includeId();
        descriptor.getChildren().put(E2.E3S.getName(), e3Descriptor);

        ObjectContext context = mockCayennePersister.newContext();
        E2 e2 = new E2();
        e2.setObjectId(new ObjectId("E2", E2.ID__PK_COLUMN, 7));
        e2.setName("XYZ");
        e2.setAddress("bla bla street");
        context.registerNewObject(e2);

        E3 e31 = new E3();
        e31.setObjectId(new ObjectId("E3", E3.ID__PK_COLUMN, 5));
        e31.setName("31");
        e31.setPhoneNumber("+12345678");
        context.registerNewObject(e31);
        e2.addToE3s(e31);

        E3 e32 = new E3();
        e32.setObjectId(new ObjectId("E3", E3.ID__PK_COLUMN, 6));
        e32.setName("32");
        e31.setPhoneNumber("+87654321");
        context.registerNewObject(e32);
        e2.addToE3s(e32);

        // saves result set in ResourceEntity
        descriptor.setResult(Collections.singletonList(e2));
        e3Descriptor.addToManyResult(new SimpleObjectId(7), e31);
        e3Descriptor.addToManyResult(new SimpleObjectId(7), e32);

        assertEquals("{\"data\":[{\"id\":7,\"e3s\":[{\"id\":5,\"name\":\"31\"},{\"id\":6,\"name\":\"32\"}]}],\"total\":1}",
                toJson(e2, descriptor));
    }

    @Test
    public void testEncoder_FilteredRoots() {

        filters.add(new EncoderFilter() {

            @Override
            public boolean matches(ResourceEntity<?> entity) {
                return true;
            }

            @Override
            public boolean encode(String propertyName, Object object, JsonGenerator out, Encoder delegate)
                    throws IOException {

                E2 e2 = (E2) object;
                if (Cayenne.intPKForObject(e2) == 7) {
                    delegate.encode(propertyName, object, out);
                    return true;
                }

                return false;
            }

            @Override
            public boolean willEncode(String propertyName, Object object, Encoder delegate) {
                E2 e2 = (E2) object;
                if (Cayenne.intPKForObject(e2) == 7) {
                    delegate.willEncode(propertyName, object);
                    return true;
                }

                return false;
            }
        });

        ResourceEntity<E2> descriptor = getResourceEntity(E2.class);
        descriptor.includeId();

        ObjectContext context = mockCayennePersister.newContext();
        E2 e21 = new E2();
        e21.setObjectId(new ObjectId("E2", E2.ID__PK_COLUMN, 7));
        e21.setName("XYZ");
        e21.setAddress("bla bla street");
        context.registerNewObject(e21);

        assertEquals("{\"data\":[{\"id\":7}],\"total\":1}", toJson(e21, descriptor));

        E2 e22 = new E2();
        e22.setObjectId(new ObjectId("E2", E2.ID__PK_COLUMN, 8));
        e22.setName("XYZ");
        e22.setAddress("bla bla street");
        context.registerNewObject(e22);

        assertEquals("{\"data\":[],\"total\":0}", toJson(e22, descriptor));
    }

    @Test
    public void testEncoder_FilteredToOne() {

        filters.add(new EncoderFilter() {

            @Override
            public boolean matches(ResourceEntity<?> entity) {
                return true;
            }

            @Override
            public boolean encode(String propertyName, Object object, JsonGenerator out, Encoder delegate)
                    throws IOException {

                if (object instanceof E2) {
                    E2 e2 = (E2) object;
                    if (Cayenne.intPKForObject(e2) == 7) {
                        return delegate.encode(propertyName, object, out);
                    } else {
                        return false;
                    }
                } else {
                    delegate.encode(propertyName, object, out);
                    return true;
                }
            }

            @Override
            public boolean willEncode(String propertyName, Object object, Encoder delegate) {
                if (object instanceof E2) {
                    E2 e2 = (E2) object;
                    if (Cayenne.intPKForObject(e2) == 7) {
                        return delegate.willEncode(propertyName, object);
                    } else {
                        return false;
                    }
                } else {
                    return delegate.willEncode(propertyName, object);
                }
            }
        });

        ResourceEntity<E2> e2Descriptor = getResourceEntity(E2.class);
        e2Descriptor.includeId();

        ResourceEntity<E3> e3Descriptor = getResourceEntity(E3.class);
        e3Descriptor.includeId();
        e3Descriptor.getChildren().put(E3.E2.getName(), e2Descriptor);

        ObjectContext context = mockCayennePersister.newContext();

        E2 e21 = new E2();
        e21.setObjectId(new ObjectId("E2", E2.ID__PK_COLUMN, 7));
        context.registerNewObject(e21);

        E3 e31 = new E3();
        e31.setObjectId(new ObjectId("E3", E3.ID__PK_COLUMN, 5));
        context.registerNewObject(e31);
        e31.setE2(e21);
        // saves result set in ResourceEntity
        e3Descriptor.setResult(Collections.singletonList(e31));
        e2Descriptor.setToOneResult(new SimpleObjectId(5), e21);

        assertEquals("{\"data\":[{\"id\":5,\"e2\":{\"id\":7}}],\"total\":1}", toJson(e31, e3Descriptor));

        E2 e22 = new E2();
        e22.setObjectId(new ObjectId("E2", E2.ID__PK_COLUMN, 8));
        context.registerNewObject(e22);

        E3 e32 = new E3();
        e32.setObjectId(new ObjectId("E3", E3.ID__PK_COLUMN, 6));
        context.registerNewObject(e32);
        e32.setE2(e22);
        // saves result set in ResourceEntity
        e3Descriptor.setResult(Collections.singletonList(e32));
        e2Descriptor.setToOneResult(new SimpleObjectId(6), e22);

        assertEquals("{\"data\":[{\"id\":6}],\"total\":1}", toJson(e32, e3Descriptor));
    }

    @Test
    public void testEncoder_FilterNoMatch() throws IOException {

        filters.add(new EncoderFilter() {

            @Override
            public boolean matches(ResourceEntity<?> entity) {
                return false;
            }

            @Override
            public boolean encode(String propertyName, Object object, JsonGenerator out, Encoder delegate) {
                fail("Non matching filter was not supposed to be invoked");
                return false;
            }

            @Override
            public boolean willEncode(String propertyName, Object object, Encoder delegate) {
                fail("Non matching filter was not supposed to be invoked");
                return false;
            }
        });

        ResourceEntity<E2> descriptor = getResourceEntity(E2.class);
        descriptor.includeId();

        DataResponse<E2> builder = DataResponse.forType(E2.class);

        ObjectContext context = mockCayennePersister.newContext();
        E2 e21 = new E2();
        e21.setObjectId(new ObjectId("E2", E2.ID__PK_COLUMN, 7));
        e21.setName("XYZ");
        e21.setAddress("bla bla street");
        context.registerNewObject(e21);

        builder.getEncoder().encode(null, Collections.singletonList(e21), mock(JsonGenerator.class));
    }

    @Test
    public void testEncoder_BinaryAttribute() {

        ResourceEntity<E19> descriptor = getResourceEntity(E19.class);
        descriptor.includeId();
        descriptor.getAttributes().put(
                E19.GUID.getName(),
                getAgEntity(E19.class).getAttribute(E19.GUID.getName())
        );

        E19 e19 = new E19();
        e19.setObjectId(new ObjectId("E19", E19.ID_PK_COLUMN, 1));
        e19.setGuid("abcdefghjklmnopr".getBytes(StandardCharsets.UTF_8));

        assertEquals("{\"data\":[{\"id\":1,\"guid\":\"YWJjZGVmZ2hqa2xtbm9wcg==\"}],\"total\":1}", toJson(e19, descriptor));
    }

    private String toJson(Object object, ResourceEntity<?> resourceEntity) {
        Encoder encoder = encoderService.dataEncoder(resourceEntity);
        return Encoders.toJson(encoder, Collections.singletonList(object));
    }
}
