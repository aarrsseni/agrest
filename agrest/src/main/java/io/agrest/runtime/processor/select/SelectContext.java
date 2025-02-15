package io.agrest.runtime.processor.select;

import io.agrest.AgRequest;
import io.agrest.CompoundObjectId;
import io.agrest.DataResponse;
import io.agrest.EntityParent;
import io.agrest.EntityProperty;
import io.agrest.AgObjectId;
import io.agrest.ResourceEntity;
import io.agrest.SimpleObjectId;
import io.agrest.SizeConstraints;
import io.agrest.constraints.Constraint;
import io.agrest.encoder.Encoder;
import io.agrest.processor.BaseProcessingContext;

import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maintains state of the request processing chain for select requests.
 *
 * @since 1.16
 */
public class SelectContext<T> extends BaseProcessingContext<T> {

    private AgObjectId id;
    private EntityParent<?> parent;
    private ResourceEntity<T> entity;
    private UriInfo uriInfo;
    private Map<String, EntityProperty> extraProperties;
    private SizeConstraints sizeConstraints;
    private Constraint<T> constraint;
    private boolean atMostOneObject;
    private Encoder encoder;
    private AgRequest mergedRequest;
    private AgRequest request;


    public SelectContext(Class<T> type) {
        super(type);
    }

    /**
     * Returns a new response object reflecting the context state.
     *
     * @return a new response object reflecting the context state.
     * @since 1.24
     */
    public DataResponse<T> createDataResponse() {
        List<? extends T> objects = this.entity != null ? this.entity.getResult() : Collections.<T>emptyList();
        DataResponse<T> response = DataResponse.forType(getType());
        response.setObjects(objects);
        response.setEncoder(encoder);
        response.setStatus(getStatus());
        return response;
    }

    public boolean isById() {
        return id != null;
    }

    public AgObjectId getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = new SimpleObjectId(id);
    }

    public void setCompoundId(Map<String, Object> ids) {
        this.id = new CompoundObjectId(ids);
    }

    public EntityParent<?> getParent() {
        return parent;
    }

    public void setParent(EntityParent<?> parent) {
        this.parent = parent;
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }

    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    /**
     * @since 2.5
     */
    public Map<String, List<String>> getProtocolParameters() {
        return uriInfo != null ? uriInfo.getQueryParameters() : Collections.emptyMap();
    }

    public Map<String, EntityProperty> getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(Map<String, EntityProperty> extraProperties) {
        this.extraProperties = extraProperties;
    }

    public SizeConstraints getSizeConstraints() {
        return sizeConstraints;
    }

    public void setSizeConstraints(SizeConstraints sizeConstraints) {
        this.sizeConstraints = sizeConstraints;
    }


    /**
     * @return this context's constraint function.
     * @since 2.4
     */
    public Constraint<T> getConstraint() {
        return constraint;
    }

    /**
     * @param constraint constraint function.
     * @since 2.4
     */
    public void setConstraint(Constraint<T> constraint) {
        this.constraint = constraint;
    }

    public boolean isAtMostOneObject() {
        return atMostOneObject;
    }

    public void setAtMostOneObject(boolean expectingOne) {
        this.atMostOneObject = expectingOne;
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    /**
     * @since 1.20
     */
    public ResourceEntity<T> getEntity() {
        return entity;
    }

    /**
     * @since 1.20
     */
    public void setEntity(ResourceEntity<T> entity) {
        this.entity = entity;
    }

    /**
     * Returns AgRequest instance that is the source of request data for {@link io.agrest.SelectStage#CREATE_ENTITY}
     * stage that produces a tree of {@link ResourceEntity} instances. Usually merged request is a result of merging
     * context AgRequest with URL parameters during {@link io.agrest.SelectStage#PARSE_REQUEST} stage.
     *
     * @since 3.2
     */
    public AgRequest getMergedRequest() {
        return mergedRequest;
    }

    /**
     * Sets AgRequest instance that is the source of request data for {@link io.agrest.SelectStage#CREATE_ENTITY} stage
     * to create a tree of {@link ResourceEntity} instances.
     *
     * @since 3.2
     */
    public void setMergedRequest(AgRequest request) {
        this.mergedRequest = request;
    }

    /**
     * Returns a request object, previously explicitly passed to the select chain in the endpoint method. Depending on
     * the calling chain configuration, this object is either used directly to serve the request, or is combined with
     * URL parameters during {@link io.agrest.SelectStage#PARSE_REQUEST}, producing a "mergedRequest".
     *
     * @since 2.13
     */
    public AgRequest getRequest() {
        return request;
    }

    /**
     * Sets a request object. Depending on the calling chain configuration, this object is either used directly to
     * serve the request, or is combined with URL parameters during {@link io.agrest.SelectStage#PARSE_REQUEST},
     * producing a "mergedRequest".
     *
     * @since 2.13
     */
    public void setRequest(AgRequest request) {
        this.request = request;
    }

    /**
     * @since 2.13
     * @deprecated since 3.2 in favor of {@link #getMergedRequest()}.
     */
    @Deprecated
    public AgRequest getRawRequest() {
        return getMergedRequest();
    }

    /**
     * @since 2.13
     * @deprecated since 3.2 in favor of {@link #setMergedRequest(AgRequest)}
     */
    @Deprecated
    public void setRawRequest(AgRequest request) {
        setMergedRequest(request);
    }
}
