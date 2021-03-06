/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.client;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.storedscripts.DeleteStoredScriptRequest;
import org.elasticsearch.action.admin.cluster.storedscripts.GetStoredScriptRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequest;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.flush.SyncedFlushRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.rollover.RolloverRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.shrink.ResizeRequest;
import org.elasticsearch.action.admin.indices.shrink.ResizeType;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.validate.query.ValidateQueryRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.explain.ExplainRequest;
import org.elasticsearch.action.fieldcaps.FieldCapabilitiesRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.security.RefreshPolicy;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.rankeval.RankEvalRequest;
import org.elasticsearch.index.reindex.AbstractBulkByScrollRequest;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.protocol.xpack.license.DeleteLicenseRequest;
import org.elasticsearch.protocol.xpack.license.GetLicenseRequest;
import org.elasticsearch.protocol.xpack.license.PutLicenseRequest;
import org.elasticsearch.protocol.xpack.watcher.DeleteWatchRequest;
import org.elasticsearch.protocol.xpack.watcher.PutWatchRequest;
import org.elasticsearch.rest.action.search.RestSearchAction;
import org.elasticsearch.script.mustache.MultiSearchTemplateRequest;
import org.elasticsearch.script.mustache.SearchTemplateRequest;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.tasks.TaskId;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.StringJoiner;

final class RequestConverters {
    static final XContentType REQUEST_BODY_CONTENT_TYPE = XContentType.JSON;

    private RequestConverters() {
        // Contains only status utility methods
    }

    static Request delete(DeleteRequest deleteRequest) {
        String endpoint = endpoint(deleteRequest.index(), deleteRequest.type(), deleteRequest.id());
        Request request = new Request(HttpDelete.METHOD_NAME, endpoint);

        Params parameters = new Params(request);
        parameters.withRouting(deleteRequest.routing());
        parameters.withTimeout(deleteRequest.timeout());
        parameters.withVersion(deleteRequest.version());
        parameters.withVersionType(deleteRequest.versionType());
        parameters.withRefreshPolicy(deleteRequest.getRefreshPolicy());
        parameters.withWaitForActiveShards(deleteRequest.waitForActiveShards());
        return request;
    }

    static Request deleteIndex(DeleteIndexRequest deleteIndexRequest) {
        String endpoint = endpoint(deleteIndexRequest.indices());
        Request request = new Request(HttpDelete.METHOD_NAME, endpoint);

        Params parameters = new Params(request);
        parameters.withTimeout(deleteIndexRequest.timeout());
        parameters.withMasterTimeout(deleteIndexRequest.masterNodeTimeout());
        parameters.withIndicesOptions(deleteIndexRequest.indicesOptions());
        return request;
    }

    static Request openIndex(OpenIndexRequest openIndexRequest) {
        String endpoint = endpoint(openIndexRequest.indices(), "_open");
        Request request = new Request(HttpPost.METHOD_NAME, endpoint);

        Params parameters = new Params(request);
        parameters.withTimeout(openIndexRequest.timeout());
        parameters.withMasterTimeout(openIndexRequest.masterNodeTimeout());
        parameters.withWaitForActiveShards(openIndexRequest.waitForActiveShards());
        parameters.withIndicesOptions(openIndexRequest.indicesOptions());
        return request;
    }

    static Request closeIndex(CloseIndexRequest closeIndexRequest) {
        String endpoint = endpoint(closeIndexRequest.indices(), "_close");
        Request request = new Request(HttpPost.METHOD_NAME, endpoint);

        Params parameters = new Params(request);
        parameters.withTimeout(closeIndexRequest.timeout());
        parameters.withMasterTimeout(closeIndexRequest.masterNodeTimeout());
        parameters.withIndicesOptions(closeIndexRequest.indicesOptions());
        return request;
    }

    static Request createIndex(CreateIndexRequest createIndexRequest) throws IOException {
        String endpoint = endpoint(createIndexRequest.indices());
        Request request = new Request(HttpPut.METHOD_NAME, endpoint);

        Params parameters = new Params(request);
        parameters.withTimeout(createIndexRequest.timeout());
        parameters.withMasterTimeout(createIndexRequest.masterNodeTimeout());
        parameters.withWaitForActiveShards(createIndexRequest.waitForActiveShards());

        request.setEntity(createEntity(createIndexRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request updateAliases(IndicesAliasesRequest indicesAliasesRequest) throws IOException {
        Request request = new Request(HttpPost.METHOD_NAME, "/_aliases");

        Params parameters = new Params(request);
        parameters.withTimeout(indicesAliasesRequest.timeout());
        parameters.withMasterTimeout(indicesAliasesRequest.masterNodeTimeout());

        request.setEntity(createEntity(indicesAliasesRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request putMapping(PutMappingRequest putMappingRequest) throws IOException {
        // The concreteIndex is an internal concept, not applicable to requests made over the REST API.
        if (putMappingRequest.getConcreteIndex() != null) {
            throw new IllegalArgumentException("concreteIndex cannot be set on PutMapping requests made over the REST API");
        }

        Request request = new Request(HttpPut.METHOD_NAME, endpoint(putMappingRequest.indices(), "_mapping", putMappingRequest.type()));

        Params parameters = new Params(request);
        parameters.withTimeout(putMappingRequest.timeout());
        parameters.withMasterTimeout(putMappingRequest.masterNodeTimeout());

        request.setEntity(createEntity(putMappingRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request getMappings(GetMappingsRequest getMappingsRequest) throws IOException {
        String[] indices = getMappingsRequest.indices() == null ? Strings.EMPTY_ARRAY : getMappingsRequest.indices();
        String[] types = getMappingsRequest.types() == null ? Strings.EMPTY_ARRAY : getMappingsRequest.types();

        Request request = new Request(HttpGet.METHOD_NAME, endpoint(indices, "_mapping", types));

        Params parameters = new Params(request);
        parameters.withMasterTimeout(getMappingsRequest.masterNodeTimeout());
        parameters.withIndicesOptions(getMappingsRequest.indicesOptions());
        parameters.withLocal(getMappingsRequest.local());
        return request;
    }

    static Request getFieldMapping(GetFieldMappingsRequest getFieldMappingsRequest) throws IOException {
        String[] indices = getFieldMappingsRequest.indices() == null ? Strings.EMPTY_ARRAY : getFieldMappingsRequest.indices();
        String[] types = getFieldMappingsRequest.types() == null ? Strings.EMPTY_ARRAY : getFieldMappingsRequest.types();
        String[] fields = getFieldMappingsRequest.fields() == null ? Strings.EMPTY_ARRAY : getFieldMappingsRequest.fields();

        String endpoint = new EndpointBuilder().addCommaSeparatedPathParts(indices)
            .addPathPartAsIs("_mapping").addCommaSeparatedPathParts(types)
            .addPathPartAsIs("field").addCommaSeparatedPathParts(fields)
            .build();

        Request request = new Request(HttpGet.METHOD_NAME, endpoint);

        Params parameters = new Params(request);
        parameters.withIndicesOptions(getFieldMappingsRequest.indicesOptions());
        parameters.withIncludeDefaults(getFieldMappingsRequest.includeDefaults());
        parameters.withLocal(getFieldMappingsRequest.local());
        return request;
    }

    static Request refresh(RefreshRequest refreshRequest) {
        String[] indices = refreshRequest.indices() == null ? Strings.EMPTY_ARRAY : refreshRequest.indices();
        Request request = new Request(HttpPost.METHOD_NAME, endpoint(indices, "_refresh"));

        Params parameters = new Params(request);
        parameters.withIndicesOptions(refreshRequest.indicesOptions());
        return request;
    }

    static Request flush(FlushRequest flushRequest) {
        String[] indices = flushRequest.indices() == null ? Strings.EMPTY_ARRAY : flushRequest.indices();
        Request request = new Request(HttpPost.METHOD_NAME, endpoint(indices, "_flush"));

        Params parameters = new Params(request);
        parameters.withIndicesOptions(flushRequest.indicesOptions());
        parameters.putParam("wait_if_ongoing", Boolean.toString(flushRequest.waitIfOngoing()));
        parameters.putParam("force", Boolean.toString(flushRequest.force()));
        return request;
    }

    static Request flushSynced(SyncedFlushRequest syncedFlushRequest) {
        String[] indices = syncedFlushRequest.indices() == null ? Strings.EMPTY_ARRAY : syncedFlushRequest.indices();
        Request request = new Request(HttpPost.METHOD_NAME, endpoint(indices, "_flush/synced"));
        Params parameters = new Params(request);
        parameters.withIndicesOptions(syncedFlushRequest.indicesOptions());
        return request;
    }

    static Request forceMerge(ForceMergeRequest forceMergeRequest) {
        String[] indices = forceMergeRequest.indices() == null ? Strings.EMPTY_ARRAY : forceMergeRequest.indices();
        Request request = new Request(HttpPost.METHOD_NAME, endpoint(indices, "_forcemerge"));

        Params parameters = new Params(request);
        parameters.withIndicesOptions(forceMergeRequest.indicesOptions());
        parameters.putParam("max_num_segments", Integer.toString(forceMergeRequest.maxNumSegments()));
        parameters.putParam("only_expunge_deletes", Boolean.toString(forceMergeRequest.onlyExpungeDeletes()));
        parameters.putParam("flush", Boolean.toString(forceMergeRequest.flush()));
        return request;
    }

    static Request clearCache(ClearIndicesCacheRequest clearIndicesCacheRequest) {
        String[] indices = clearIndicesCacheRequest.indices() == null ? Strings.EMPTY_ARRAY :clearIndicesCacheRequest.indices();
        Request request = new Request(HttpPost.METHOD_NAME, endpoint(indices, "_cache/clear"));

        Params parameters = new Params(request);
        parameters.withIndicesOptions(clearIndicesCacheRequest.indicesOptions());
        parameters.putParam("query", Boolean.toString(clearIndicesCacheRequest.queryCache()));
        parameters.putParam("fielddata", Boolean.toString(clearIndicesCacheRequest.fieldDataCache()));
        parameters.putParam("request", Boolean.toString(clearIndicesCacheRequest.requestCache()));
        parameters.putParam("fields", String.join(",", clearIndicesCacheRequest.fields()));
        return request;
    }

    static Request info() {
        return new Request(HttpGet.METHOD_NAME, "/");
    }

    static Request bulk(BulkRequest bulkRequest) throws IOException {
        Request request = new Request(HttpPost.METHOD_NAME, "/_bulk");

        Params parameters = new Params(request);
        parameters.withTimeout(bulkRequest.timeout());
        parameters.withRefreshPolicy(bulkRequest.getRefreshPolicy());

        // Bulk API only supports newline delimited JSON or Smile. Before executing
        // the bulk, we need to check that all requests have the same content-type
        // and this content-type is supported by the Bulk API.
        XContentType bulkContentType = null;
        for (int i = 0; i < bulkRequest.numberOfActions(); i++) {
            DocWriteRequest<?> action = bulkRequest.requests().get(i);

            DocWriteRequest.OpType opType = action.opType();
            if (opType == DocWriteRequest.OpType.INDEX || opType == DocWriteRequest.OpType.CREATE) {
                bulkContentType = enforceSameContentType((IndexRequest) action, bulkContentType);

            } else if (opType == DocWriteRequest.OpType.UPDATE) {
                UpdateRequest updateRequest = (UpdateRequest) action;
                if (updateRequest.doc() != null) {
                    bulkContentType = enforceSameContentType(updateRequest.doc(), bulkContentType);
                }
                if (updateRequest.upsertRequest() != null) {
                    bulkContentType = enforceSameContentType(updateRequest.upsertRequest(), bulkContentType);
                }
            }
        }

        if (bulkContentType == null) {
            bulkContentType = XContentType.JSON;
        }

        final byte separator = bulkContentType.xContent().streamSeparator();
        final ContentType requestContentType = createContentType(bulkContentType);

        ByteArrayOutputStream content = new ByteArrayOutputStream();
        for (DocWriteRequest<?> action : bulkRequest.requests()) {
            DocWriteRequest.OpType opType = action.opType();

            try (XContentBuilder metadata = XContentBuilder.builder(bulkContentType.xContent())) {
                metadata.startObject();
                {
                    metadata.startObject(opType.getLowercase());
                    if (Strings.hasLength(action.index())) {
                        metadata.field("_index", action.index());
                    }
                    if (Strings.hasLength(action.type())) {
                        metadata.field("_type", action.type());
                    }
                    if (Strings.hasLength(action.id())) {
                        metadata.field("_id", action.id());
                    }
                    if (Strings.hasLength(action.routing())) {
                        metadata.field("routing", action.routing());
                    }
                    if (action.version() != Versions.MATCH_ANY) {
                        metadata.field("version", action.version());
                    }

                    VersionType versionType = action.versionType();
                    if (versionType != VersionType.INTERNAL) {
                        if (versionType == VersionType.EXTERNAL) {
                            metadata.field("version_type", "external");
                        } else if (versionType == VersionType.EXTERNAL_GTE) {
                            metadata.field("version_type", "external_gte");
                        } else if (versionType == VersionType.FORCE) {
                            metadata.field("version_type", "force");
                        }
                    }

                    if (opType == DocWriteRequest.OpType.INDEX || opType == DocWriteRequest.OpType.CREATE) {
                        IndexRequest indexRequest = (IndexRequest) action;
                        if (Strings.hasLength(indexRequest.getPipeline())) {
                            metadata.field("pipeline", indexRequest.getPipeline());
                        }
                    } else if (opType == DocWriteRequest.OpType.UPDATE) {
                        UpdateRequest updateRequest = (UpdateRequest) action;
                        if (updateRequest.retryOnConflict() > 0) {
                            metadata.field("retry_on_conflict", updateRequest.retryOnConflict());
                        }
                        if (updateRequest.fetchSource() != null) {
                            metadata.field("_source", updateRequest.fetchSource());
                        }
                    }
                    metadata.endObject();
                }
                metadata.endObject();

                BytesRef metadataSource = BytesReference.bytes(metadata).toBytesRef();
                content.write(metadataSource.bytes, metadataSource.offset, metadataSource.length);
                content.write(separator);
            }

            BytesRef source = null;
            if (opType == DocWriteRequest.OpType.INDEX || opType == DocWriteRequest.OpType.CREATE) {
                IndexRequest indexRequest = (IndexRequest) action;
                BytesReference indexSource = indexRequest.source();
                XContentType indexXContentType = indexRequest.getContentType();

                try (XContentParser parser = XContentHelper.createParser(
                        /*
                         * EMPTY and THROW are fine here because we just call
                         * copyCurrentStructure which doesn't touch the
                         * registry or deprecation.
                         */
                        NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                        indexSource, indexXContentType)) {
                    try (XContentBuilder builder = XContentBuilder.builder(bulkContentType.xContent())) {
                        builder.copyCurrentStructure(parser);
                        source = BytesReference.bytes(builder).toBytesRef();
                    }
                }
            } else if (opType == DocWriteRequest.OpType.UPDATE) {
                source = XContentHelper.toXContent((UpdateRequest) action, bulkContentType, false).toBytesRef();
            }

            if (source != null) {
                content.write(source.bytes, source.offset, source.length);
                content.write(separator);
            }
        }
        request.setEntity(new ByteArrayEntity(content.toByteArray(), 0, content.size(), requestContentType));
        return request;
    }

    static Request exists(GetRequest getRequest) {
        return getStyleRequest(HttpHead.METHOD_NAME, getRequest);
    }

    static Request get(GetRequest getRequest) {
        return getStyleRequest(HttpGet.METHOD_NAME, getRequest);
    }

    private static Request getStyleRequest(String method, GetRequest getRequest) {
        Request request = new Request(method, endpoint(getRequest.index(), getRequest.type(), getRequest.id()));

        Params parameters = new Params(request);
        parameters.withPreference(getRequest.preference());
        parameters.withRouting(getRequest.routing());
        parameters.withRefresh(getRequest.refresh());
        parameters.withRealtime(getRequest.realtime());
        parameters.withStoredFields(getRequest.storedFields());
        parameters.withVersion(getRequest.version());
        parameters.withVersionType(getRequest.versionType());
        parameters.withFetchSourceContext(getRequest.fetchSourceContext());

        return request;
    }

    static Request multiGet(MultiGetRequest multiGetRequest) throws IOException {
        Request request = new Request(HttpPost.METHOD_NAME, "/_mget");

        Params parameters = new Params(request);
        parameters.withPreference(multiGetRequest.preference());
        parameters.withRealtime(multiGetRequest.realtime());
        parameters.withRefresh(multiGetRequest.refresh());

        request.setEntity(createEntity(multiGetRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request index(IndexRequest indexRequest) {
        String method = Strings.hasLength(indexRequest.id()) ? HttpPut.METHOD_NAME : HttpPost.METHOD_NAME;
        boolean isCreate = (indexRequest.opType() == DocWriteRequest.OpType.CREATE);
        String endpoint = endpoint(indexRequest.index(), indexRequest.type(), indexRequest.id(), isCreate ? "_create" : null);
        Request request = new Request(method, endpoint);

        Params parameters = new Params(request);
        parameters.withRouting(indexRequest.routing());
        parameters.withTimeout(indexRequest.timeout());
        parameters.withVersion(indexRequest.version());
        parameters.withVersionType(indexRequest.versionType());
        parameters.withPipeline(indexRequest.getPipeline());
        parameters.withRefreshPolicy(indexRequest.getRefreshPolicy());
        parameters.withWaitForActiveShards(indexRequest.waitForActiveShards());

        BytesRef source = indexRequest.source().toBytesRef();
        ContentType contentType = createContentType(indexRequest.getContentType());
        request.setEntity(new ByteArrayEntity(source.bytes, source.offset, source.length, contentType));
        return request;
    }

    static Request ping() {
        return new Request(HttpHead.METHOD_NAME, "/");
    }

    static Request update(UpdateRequest updateRequest) throws IOException {
        String endpoint = endpoint(updateRequest.index(), updateRequest.type(), updateRequest.id(), "_update");
        Request request = new Request(HttpPost.METHOD_NAME, endpoint);

        Params parameters = new Params(request);
        parameters.withRouting(updateRequest.routing());
        parameters.withTimeout(updateRequest.timeout());
        parameters.withRefreshPolicy(updateRequest.getRefreshPolicy());
        parameters.withWaitForActiveShards(updateRequest.waitForActiveShards());
        parameters.withDocAsUpsert(updateRequest.docAsUpsert());
        parameters.withFetchSourceContext(updateRequest.fetchSource());
        parameters.withRetryOnConflict(updateRequest.retryOnConflict());
        parameters.withVersion(updateRequest.version());
        parameters.withVersionType(updateRequest.versionType());

        // The Java API allows update requests with different content types
        // set for the partial document and the upsert document. This client
        // only accepts update requests that have the same content types set
        // for both doc and upsert.
        XContentType xContentType = null;
        if (updateRequest.doc() != null) {
            xContentType = updateRequest.doc().getContentType();
        }
        if (updateRequest.upsertRequest() != null) {
            XContentType upsertContentType = updateRequest.upsertRequest().getContentType();
            if ((xContentType != null) && (xContentType != upsertContentType)) {
                throw new IllegalStateException("Update request cannot have different content types for doc [" + xContentType + "]" +
                        " and upsert [" + upsertContentType + "] documents");
            } else {
                xContentType = upsertContentType;
            }
        }
        if (xContentType == null) {
            xContentType = Requests.INDEX_CONTENT_TYPE;
        }
        request.setEntity(createEntity(updateRequest, xContentType));
        return request;
    }

    static Request search(SearchRequest searchRequest) throws IOException {
        Request request = new Request(HttpPost.METHOD_NAME, endpoint(searchRequest.indices(), searchRequest.types(), "_search"));

        Params params = new Params(request);
        addSearchRequestParams(params, searchRequest);

        if (searchRequest.source() != null) {
            request.setEntity(createEntity(searchRequest.source(), REQUEST_BODY_CONTENT_TYPE));
        }
        return request;
    }

    private static void addSearchRequestParams(Params params, SearchRequest searchRequest) {
        params.putParam(RestSearchAction.TYPED_KEYS_PARAM, "true");
        params.withRouting(searchRequest.routing());
        params.withPreference(searchRequest.preference());
        params.withIndicesOptions(searchRequest.indicesOptions());
        params.putParam("search_type", searchRequest.searchType().name().toLowerCase(Locale.ROOT));
        if (searchRequest.requestCache() != null) {
            params.putParam("request_cache", Boolean.toString(searchRequest.requestCache()));
        }
        if (searchRequest.allowPartialSearchResults() != null) {
            params.putParam("allow_partial_search_results", Boolean.toString(searchRequest.allowPartialSearchResults()));
        }
        params.putParam("batched_reduce_size", Integer.toString(searchRequest.getBatchedReduceSize()));
        if (searchRequest.scroll() != null) {
            params.putParam("scroll", searchRequest.scroll().keepAlive());
        }
    }

    static Request searchScroll(SearchScrollRequest searchScrollRequest) throws IOException {
        Request request = new Request(HttpPost.METHOD_NAME, "/_search/scroll");
        request.setEntity(createEntity(searchScrollRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request clearScroll(ClearScrollRequest clearScrollRequest) throws IOException {
        Request request = new Request(HttpDelete.METHOD_NAME, "/_search/scroll");
        request.setEntity(createEntity(clearScrollRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request multiSearch(MultiSearchRequest multiSearchRequest) throws IOException {
        Request request = new Request(HttpPost.METHOD_NAME, "/_msearch");

        Params params = new Params(request);
        params.putParam(RestSearchAction.TYPED_KEYS_PARAM, "true");
        if (multiSearchRequest.maxConcurrentSearchRequests() != MultiSearchRequest.MAX_CONCURRENT_SEARCH_REQUESTS_DEFAULT) {
            params.putParam("max_concurrent_searches", Integer.toString(multiSearchRequest.maxConcurrentSearchRequests()));
        }

        XContent xContent = REQUEST_BODY_CONTENT_TYPE.xContent();
        byte[] source = MultiSearchRequest.writeMultiLineFormat(multiSearchRequest, xContent);
        request.setEntity(new ByteArrayEntity(source, createContentType(xContent.type())));
        return request;
    }

    static Request searchTemplate(SearchTemplateRequest searchTemplateRequest) throws IOException {
        Request request;

        if (searchTemplateRequest.isSimulate()) {
            request = new Request(HttpGet.METHOD_NAME, "_render/template");
        } else {
            SearchRequest searchRequest = searchTemplateRequest.getRequest();
            String endpoint = endpoint(searchRequest.indices(), searchRequest.types(), "_search/template");
            request = new Request(HttpGet.METHOD_NAME, endpoint);

            Params params = new Params(request);
            addSearchRequestParams(params, searchRequest);
        }

        request.setEntity(createEntity(searchTemplateRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request multiSearchTemplate(MultiSearchTemplateRequest multiSearchTemplateRequest) throws IOException {
        Request request = new Request(HttpPost.METHOD_NAME, "/_msearch/template");

        Params params = new Params(request);
        params.putParam(RestSearchAction.TYPED_KEYS_PARAM, "true");
        if (multiSearchTemplateRequest.maxConcurrentSearchRequests() != MultiSearchRequest.MAX_CONCURRENT_SEARCH_REQUESTS_DEFAULT) {
            params.putParam("max_concurrent_searches", Integer.toString(multiSearchTemplateRequest.maxConcurrentSearchRequests()));
        }

        XContent xContent = REQUEST_BODY_CONTENT_TYPE.xContent();
        byte[] source = MultiSearchTemplateRequest.writeMultiLineFormat(multiSearchTemplateRequest, xContent);
        request.setEntity(new ByteArrayEntity(source, createContentType(xContent.type())));
        return request;
    }

    static Request existsAlias(GetAliasesRequest getAliasesRequest) {
        if ((getAliasesRequest.indices() == null || getAliasesRequest.indices().length == 0) &&
                (getAliasesRequest.aliases() == null || getAliasesRequest.aliases().length == 0)) {
            throw new IllegalArgumentException("existsAlias requires at least an alias or an index");
        }
        String[] indices = getAliasesRequest.indices() == null ? Strings.EMPTY_ARRAY : getAliasesRequest.indices();
        String[] aliases = getAliasesRequest.aliases() == null ? Strings.EMPTY_ARRAY : getAliasesRequest.aliases();

        Request request = new Request(HttpHead.METHOD_NAME, endpoint(indices, "_alias", aliases));

        Params params = new Params(request);
        params.withIndicesOptions(getAliasesRequest.indicesOptions());
        params.withLocal(getAliasesRequest.local());
        return request;
    }

    static Request explain(ExplainRequest explainRequest) throws IOException {
        Request request = new Request(HttpGet.METHOD_NAME,
            endpoint(explainRequest.index(), explainRequest.type(), explainRequest.id(), "_explain"));

        Params params = new Params(request);
        params.withStoredFields(explainRequest.storedFields());
        params.withFetchSourceContext(explainRequest.fetchSourceContext());
        params.withRouting(explainRequest.routing());
        params.withPreference(explainRequest.preference());
        request.setEntity(createEntity(explainRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request fieldCaps(FieldCapabilitiesRequest fieldCapabilitiesRequest) {
        Request request = new Request(HttpGet.METHOD_NAME, endpoint(fieldCapabilitiesRequest.indices(), "_field_caps"));

        Params params = new Params(request);
        params.withFields(fieldCapabilitiesRequest.fields());
        params.withIndicesOptions(fieldCapabilitiesRequest.indicesOptions());
        return request;
    }

    static Request rankEval(RankEvalRequest rankEvalRequest) throws IOException {
        Request request = new Request(HttpGet.METHOD_NAME, endpoint(rankEvalRequest.indices(), Strings.EMPTY_ARRAY, "_rank_eval"));

        Params params = new Params(request);
        params.withIndicesOptions(rankEvalRequest.indicesOptions());

        request.setEntity(createEntity(rankEvalRequest.getRankEvalSpec(), REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request split(ResizeRequest resizeRequest) throws IOException {
        if (resizeRequest.getResizeType() != ResizeType.SPLIT) {
            throw new IllegalArgumentException("Wrong resize type [" + resizeRequest.getResizeType() + "] for indices split request");
        }
        return resize(resizeRequest);
    }

    static Request shrink(ResizeRequest resizeRequest) throws IOException {
        if (resizeRequest.getResizeType() != ResizeType.SHRINK) {
            throw new IllegalArgumentException("Wrong resize type [" + resizeRequest.getResizeType() + "] for indices shrink request");
        }
        return resize(resizeRequest);
    }

    private static Request resize(ResizeRequest resizeRequest) throws IOException {
        String endpoint = new EndpointBuilder().addPathPart(resizeRequest.getSourceIndex())
                .addPathPartAsIs("_" + resizeRequest.getResizeType().name().toLowerCase(Locale.ROOT))
                .addPathPart(resizeRequest.getTargetIndexRequest().index()).build();
        Request request = new Request(HttpPut.METHOD_NAME, endpoint);

        Params params = new Params(request);
        params.withTimeout(resizeRequest.timeout());
        params.withMasterTimeout(resizeRequest.masterNodeTimeout());
        params.withWaitForActiveShards(resizeRequest.getTargetIndexRequest().waitForActiveShards());

        request.setEntity(createEntity(resizeRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request reindex(ReindexRequest reindexRequest) throws IOException {
        String endpoint = new EndpointBuilder().addPathPart("_reindex").build();
        Request request = new Request(HttpPost.METHOD_NAME, endpoint);
        Params params = new Params(request)
            .withRefresh(reindexRequest.isRefresh())
            .withTimeout(reindexRequest.getTimeout())
            .withWaitForActiveShards(reindexRequest.getWaitForActiveShards());

        if (reindexRequest.getScrollTime() != null) {
            params.putParam("scroll", reindexRequest.getScrollTime());
        }
        request.setEntity(createEntity(reindexRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request updateByQuery(UpdateByQueryRequest updateByQueryRequest) throws IOException {
        String endpoint =
            endpoint(updateByQueryRequest.indices(), updateByQueryRequest.getDocTypes(), "_update_by_query");
        Request request = new Request(HttpPost.METHOD_NAME, endpoint);
        Params params = new Params(request)
            .withRouting(updateByQueryRequest.getRouting())
            .withPipeline(updateByQueryRequest.getPipeline())
            .withRefresh(updateByQueryRequest.isRefresh())
            .withTimeout(updateByQueryRequest.getTimeout())
            .withWaitForActiveShards(updateByQueryRequest.getWaitForActiveShards())
            .withIndicesOptions(updateByQueryRequest.indicesOptions());
        if (updateByQueryRequest.isAbortOnVersionConflict() == false) {
            params.putParam("conflicts", "proceed");
        }
        if (updateByQueryRequest.getBatchSize() != AbstractBulkByScrollRequest.DEFAULT_SCROLL_SIZE) {
            params.putParam("scroll_size", Integer.toString(updateByQueryRequest.getBatchSize()));
        }
        if (updateByQueryRequest.getScrollTime() != AbstractBulkByScrollRequest.DEFAULT_SCROLL_TIMEOUT) {
            params.putParam("scroll", updateByQueryRequest.getScrollTime());
        }
        if (updateByQueryRequest.getSize() > 0) {
            params.putParam("size", Integer.toString(updateByQueryRequest.getSize()));
        }
        request.setEntity(createEntity(updateByQueryRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request deleteByQuery(DeleteByQueryRequest deleteByQueryRequest) throws IOException {
        String endpoint =
            endpoint(deleteByQueryRequest.indices(), deleteByQueryRequest.getDocTypes(), "_delete_by_query");
        Request request = new Request(HttpPost.METHOD_NAME, endpoint);
        Params params = new Params(request)
            .withRouting(deleteByQueryRequest.getRouting())
            .withRefresh(deleteByQueryRequest.isRefresh())
            .withTimeout(deleteByQueryRequest.getTimeout())
            .withWaitForActiveShards(deleteByQueryRequest.getWaitForActiveShards())
            .withIndicesOptions(deleteByQueryRequest.indicesOptions());
        if (deleteByQueryRequest.isAbortOnVersionConflict() == false) {
            params.putParam("conflicts", "proceed");
        }
        if (deleteByQueryRequest.getBatchSize() != AbstractBulkByScrollRequest.DEFAULT_SCROLL_SIZE) {
            params.putParam("scroll_size", Integer.toString(deleteByQueryRequest.getBatchSize()));
        }
        if (deleteByQueryRequest.getScrollTime() != AbstractBulkByScrollRequest.DEFAULT_SCROLL_TIMEOUT) {
            params.putParam("scroll", deleteByQueryRequest.getScrollTime());
        }
        if (deleteByQueryRequest.getSize() > 0) {
            params.putParam("size", Integer.toString(deleteByQueryRequest.getSize()));
        }
        request.setEntity(createEntity(deleteByQueryRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request rollover(RolloverRequest rolloverRequest) throws IOException {
        String endpoint = new EndpointBuilder().addPathPart(rolloverRequest.getAlias()).addPathPartAsIs("_rollover")
                .addPathPart(rolloverRequest.getNewIndexName()).build();
        Request request = new Request(HttpPost.METHOD_NAME, endpoint);

        Params params = new Params(request);
        params.withTimeout(rolloverRequest.timeout());
        params.withMasterTimeout(rolloverRequest.masterNodeTimeout());
        params.withWaitForActiveShards(rolloverRequest.getCreateIndexRequest().waitForActiveShards());
        if (rolloverRequest.isDryRun()) {
            params.putParam("dry_run", Boolean.TRUE.toString());
        }

        request.setEntity(createEntity(rolloverRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request getSettings(GetSettingsRequest getSettingsRequest) {
        String[] indices = getSettingsRequest.indices() == null ? Strings.EMPTY_ARRAY : getSettingsRequest.indices();
        String[] names = getSettingsRequest.names() == null ? Strings.EMPTY_ARRAY : getSettingsRequest.names();

        String endpoint = endpoint(indices, "_settings", names);
        Request request = new Request(HttpGet.METHOD_NAME, endpoint);

        Params params = new Params(request);
        params.withIndicesOptions(getSettingsRequest.indicesOptions());
        params.withLocal(getSettingsRequest.local());
        params.withIncludeDefaults(getSettingsRequest.includeDefaults());
        params.withMasterTimeout(getSettingsRequest.masterNodeTimeout());

        return request;
    }

    static Request getIndex(GetIndexRequest getIndexRequest) {
        String[] indices = getIndexRequest.indices() == null ? Strings.EMPTY_ARRAY : getIndexRequest.indices();

        String endpoint = endpoint(indices);
        Request request = new Request(HttpGet.METHOD_NAME, endpoint);

        Params params = new Params(request);
        params.withIndicesOptions(getIndexRequest.indicesOptions());
        params.withLocal(getIndexRequest.local());
        params.withIncludeDefaults(getIndexRequest.includeDefaults());
        params.withHuman(getIndexRequest.humanReadable());
        params.withMasterTimeout(getIndexRequest.masterNodeTimeout());

        return request;
    }

    static Request indicesExist(GetIndexRequest getIndexRequest) {
        // this can be called with no indices as argument by transport client, not via REST though
        if (getIndexRequest.indices() == null || getIndexRequest.indices().length == 0) {
            throw new IllegalArgumentException("indices are mandatory");
        }
        String endpoint = endpoint(getIndexRequest.indices(), "");
        Request request = new Request(HttpHead.METHOD_NAME, endpoint);

        Params params = new Params(request);
        params.withLocal(getIndexRequest.local());
        params.withHuman(getIndexRequest.humanReadable());
        params.withIndicesOptions(getIndexRequest.indicesOptions());
        params.withIncludeDefaults(getIndexRequest.includeDefaults());
        return request;
    }

    static Request indexPutSettings(UpdateSettingsRequest updateSettingsRequest) throws IOException {
        String[] indices = updateSettingsRequest.indices() == null ? Strings.EMPTY_ARRAY : updateSettingsRequest.indices();
        Request request = new Request(HttpPut.METHOD_NAME, endpoint(indices, "_settings"));

        Params parameters = new Params(request);
        parameters.withTimeout(updateSettingsRequest.timeout());
        parameters.withMasterTimeout(updateSettingsRequest.masterNodeTimeout());
        parameters.withIndicesOptions(updateSettingsRequest.indicesOptions());
        parameters.withPreserveExisting(updateSettingsRequest.isPreserveExisting());

        request.setEntity(createEntity(updateSettingsRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request putTemplate(PutIndexTemplateRequest putIndexTemplateRequest) throws IOException {
        String endpoint = new EndpointBuilder().addPathPartAsIs("_template").addPathPart(putIndexTemplateRequest.name()).build();
        Request request = new Request(HttpPut.METHOD_NAME, endpoint);
        Params params = new Params(request);
        params.withMasterTimeout(putIndexTemplateRequest.masterNodeTimeout());
        if (putIndexTemplateRequest.create()) {
            params.putParam("create", Boolean.TRUE.toString());
        }
        if (Strings.hasText(putIndexTemplateRequest.cause())) {
            params.putParam("cause", putIndexTemplateRequest.cause());
        }
        request.setEntity(createEntity(putIndexTemplateRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request validateQuery(ValidateQueryRequest validateQueryRequest) throws IOException {
        String[] indices = validateQueryRequest.indices() == null ? Strings.EMPTY_ARRAY : validateQueryRequest.indices();
        String[] types = validateQueryRequest.types() == null || indices.length <= 0 ? Strings.EMPTY_ARRAY : validateQueryRequest.types();
        String endpoint = endpoint(indices, types, "_validate/query");
        Request request = new Request(HttpGet.METHOD_NAME, endpoint);
        Params params = new Params(request);
        params.withIndicesOptions(validateQueryRequest.indicesOptions());
        params.putParam("explain", Boolean.toString(validateQueryRequest.explain()));
        params.putParam("all_shards", Boolean.toString(validateQueryRequest.allShards()));
        params.putParam("rewrite", Boolean.toString(validateQueryRequest.rewrite()));
        request.setEntity(createEntity(validateQueryRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request getAlias(GetAliasesRequest getAliasesRequest) {
        String[] indices = getAliasesRequest.indices() == null ? Strings.EMPTY_ARRAY : getAliasesRequest.indices();
        String[] aliases = getAliasesRequest.aliases() == null ? Strings.EMPTY_ARRAY : getAliasesRequest.aliases();
        String endpoint = endpoint(indices, "_alias", aliases);
        Request request = new Request(HttpGet.METHOD_NAME, endpoint);
        Params params = new Params(request);
        params.withIndicesOptions(getAliasesRequest.indicesOptions());
        params.withLocal(getAliasesRequest.local());
        return request;
    }

    static Request getTemplates(GetIndexTemplatesRequest getIndexTemplatesRequest) throws IOException {
        String[] names = getIndexTemplatesRequest.names();
        String endpoint = new EndpointBuilder().addPathPartAsIs("_template").addCommaSeparatedPathParts(names).build();
        Request request = new Request(HttpGet.METHOD_NAME, endpoint);
        Params params = new Params(request);
        params.withLocal(getIndexTemplatesRequest.local());
        params.withMasterTimeout(getIndexTemplatesRequest.masterNodeTimeout());
        return request;
    }

    static Request analyze(AnalyzeRequest request) throws IOException {
        EndpointBuilder builder = new EndpointBuilder();
        String index = request.index();
        if (index != null) {
            builder.addPathPart(index);
        }
        builder.addPathPartAsIs("_analyze");
        Request req = new Request(HttpGet.METHOD_NAME, builder.build());
        req.setEntity(createEntity(request, REQUEST_BODY_CONTENT_TYPE));
        return req;
    }

    static Request getScript(GetStoredScriptRequest getStoredScriptRequest) {
        String endpoint = new EndpointBuilder().addPathPartAsIs("_scripts").addPathPart(getStoredScriptRequest.id()).build();
        Request request = new Request(HttpGet.METHOD_NAME, endpoint);
        Params params = new Params(request);
        params.withMasterTimeout(getStoredScriptRequest.masterNodeTimeout());
        return request;
    }

    static Request deleteScript(DeleteStoredScriptRequest deleteStoredScriptRequest) {
        String endpoint = new EndpointBuilder().addPathPartAsIs("_scripts").addPathPart(deleteStoredScriptRequest.id()).build();
        Request request = new Request(HttpDelete.METHOD_NAME, endpoint);
        Params params = new Params(request);
        params.withTimeout(deleteStoredScriptRequest.timeout());
        params.withMasterTimeout(deleteStoredScriptRequest.masterNodeTimeout());
        return request;
    }

    static Request xPackWatcherPutWatch(PutWatchRequest putWatchRequest) {
        String endpoint = new EndpointBuilder()
            .addPathPartAsIs("_xpack")
            .addPathPartAsIs("watcher")
            .addPathPartAsIs("watch")
            .addPathPart(putWatchRequest.getId())
            .build();

        Request request = new Request(HttpPut.METHOD_NAME, endpoint);
        Params params = new Params(request).withVersion(putWatchRequest.getVersion());
        if (putWatchRequest.isActive() == false) {
            params.putParam("active", "false");
        }
        ContentType contentType = createContentType(putWatchRequest.xContentType());
        BytesReference source = putWatchRequest.getSource();
        request.setEntity(new ByteArrayEntity(source.toBytesRef().bytes, 0, source.length(), contentType));
        return request;
    }

    static Request xPackWatcherDeleteWatch(DeleteWatchRequest deleteWatchRequest) {
        String endpoint = new EndpointBuilder()
            .addPathPartAsIs("_xpack")
            .addPathPartAsIs("watcher")
            .addPathPartAsIs("watch")
            .addPathPart(deleteWatchRequest.getId())
            .build();

        Request request = new Request(HttpDelete.METHOD_NAME, endpoint);
        return request;
    }

    static Request putLicense(PutLicenseRequest putLicenseRequest) {
        String endpoint = new EndpointBuilder()
            .addPathPartAsIs("_xpack")
            .addPathPartAsIs("license")
            .build();
        Request request = new Request(HttpPut.METHOD_NAME, endpoint);
        Params parameters = new Params(request);
        parameters.withTimeout(putLicenseRequest.timeout());
        parameters.withMasterTimeout(putLicenseRequest.masterNodeTimeout());
        if (putLicenseRequest.isAcknowledge()) {
            parameters.putParam("acknowledge", "true");
        }
        request.setJsonEntity(putLicenseRequest.getLicenseDefinition());
        return request;
    }

    static Request getLicense(GetLicenseRequest getLicenseRequest) {
        String endpoint = new EndpointBuilder()
            .addPathPartAsIs("_xpack")
            .addPathPartAsIs("license")
            .build();
        Request request = new Request(HttpGet.METHOD_NAME, endpoint);
        Params parameters = new Params(request);
        parameters.withLocal(getLicenseRequest.local());
        return request;
    }

    static Request deleteLicense(DeleteLicenseRequest deleteLicenseRequest) {
        Request request = new Request(HttpDelete.METHOD_NAME, "/_xpack/license");
        Params parameters = new Params(request);
        parameters.withTimeout(deleteLicenseRequest.timeout());
        parameters.withMasterTimeout(deleteLicenseRequest.masterNodeTimeout());
        return request;
    }

    static HttpEntity createEntity(ToXContent toXContent, XContentType xContentType) throws IOException {
        BytesRef source = XContentHelper.toXContent(toXContent, xContentType, false).toBytesRef();
        return new ByteArrayEntity(source.bytes, source.offset, source.length, createContentType(xContentType));
    }

    static String endpoint(String index, String type, String id) {
        return new EndpointBuilder().addPathPart(index, type, id).build();
    }

    static String endpoint(String index, String type, String id, String endpoint) {
        return new EndpointBuilder().addPathPart(index, type, id).addPathPartAsIs(endpoint).build();
    }

    static String endpoint(String[] indices) {
        return new EndpointBuilder().addCommaSeparatedPathParts(indices).build();
    }

    static String endpoint(String[] indices, String endpoint) {
        return new EndpointBuilder().addCommaSeparatedPathParts(indices).addPathPartAsIs(endpoint).build();
    }

    static String endpoint(String[] indices, String[] types, String endpoint) {
        return new EndpointBuilder().addCommaSeparatedPathParts(indices).addCommaSeparatedPathParts(types)
                .addPathPartAsIs(endpoint).build();
    }

    static String endpoint(String[] indices, String endpoint, String[] suffixes) {
        return new EndpointBuilder().addCommaSeparatedPathParts(indices).addPathPartAsIs(endpoint)
                .addCommaSeparatedPathParts(suffixes).build();
    }

    static String endpoint(String[] indices, String endpoint, String type) {
        return new EndpointBuilder().addCommaSeparatedPathParts(indices).addPathPartAsIs(endpoint).addPathPart(type).build();
    }

    /**
     * Returns a {@link ContentType} from a given {@link XContentType}.
     *
     * @param xContentType the {@link XContentType}
     * @return the {@link ContentType}
     */
    @SuppressForbidden(reason = "Only allowed place to convert a XContentType to a ContentType")
    public static ContentType createContentType(final XContentType xContentType) {
        return ContentType.create(xContentType.mediaTypeWithoutParameters(), (Charset) null);
    }

    /**
     * Utility class to help with common parameter names and patterns. Wraps
     * a {@link Request} and adds the parameters to it directly.
     */
    static class Params {
        private final Request request;

        Params(Request request) {
            this.request = request;
        }

        Params putParam(String name, String value) {
            if (Strings.hasLength(value)) {
                request.addParameter(name, value);
            }
            return this;
        }

        Params putParam(String key, TimeValue value) {
            if (value != null) {
                return putParam(key, value.getStringRep());
            }
            return this;
        }

        Params withDocAsUpsert(boolean docAsUpsert) {
            if (docAsUpsert) {
                return putParam("doc_as_upsert", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withFetchSourceContext(FetchSourceContext fetchSourceContext) {
            if (fetchSourceContext != null) {
                if (fetchSourceContext.fetchSource() == false) {
                    putParam("_source", Boolean.FALSE.toString());
                }
                if (fetchSourceContext.includes() != null && fetchSourceContext.includes().length > 0) {
                    putParam("_source_include", String.join(",", fetchSourceContext.includes()));
                }
                if (fetchSourceContext.excludes() != null && fetchSourceContext.excludes().length > 0) {
                    putParam("_source_exclude", String.join(",", fetchSourceContext.excludes()));
                }
            }
            return this;
        }

        Params withFields(String[] fields) {
            if (fields != null && fields.length > 0) {
                return putParam("fields", String.join(",", fields));
            }
            return this;
        }

        Params withMasterTimeout(TimeValue masterTimeout) {
            return putParam("master_timeout", masterTimeout);
        }

        Params withPipeline(String pipeline) {
            return putParam("pipeline", pipeline);
        }

        Params withPreference(String preference) {
            return putParam("preference", preference);
        }

        Params withRealtime(boolean realtime) {
            if (realtime == false) {
                return putParam("realtime", Boolean.FALSE.toString());
            }
            return this;
        }

        Params withRefresh(boolean refresh) {
            if (refresh) {
                return withRefreshPolicy(RefreshPolicy.IMMEDIATE);
            }
            return this;
        }

        /**
         *  @deprecated If creating a new HLRC ReST API call, use {@link RefreshPolicy}
         *  instead of {@link WriteRequest.RefreshPolicy} from the server project
         */
        @Deprecated
        Params withRefreshPolicy(WriteRequest.RefreshPolicy refreshPolicy) {
            if (refreshPolicy != WriteRequest.RefreshPolicy.NONE) {
                return putParam("refresh", refreshPolicy.getValue());
            }
            return this;
        }

        Params withRefreshPolicy(RefreshPolicy refreshPolicy) {
            if (refreshPolicy != RefreshPolicy.NONE) {
                return putParam("refresh", refreshPolicy.getValue());
            }
            return this;
        }

        Params withRetryOnConflict(int retryOnConflict) {
            if (retryOnConflict > 0) {
                return putParam("retry_on_conflict", String.valueOf(retryOnConflict));
            }
            return this;
        }

        Params withRouting(String routing) {
            return putParam("routing", routing);
        }

        Params withStoredFields(String[] storedFields) {
            if (storedFields != null && storedFields.length > 0) {
                return putParam("stored_fields", String.join(",", storedFields));
            }
            return this;
        }

        Params withTimeout(TimeValue timeout) {
            return putParam("timeout", timeout);
        }

        Params withVersion(long version) {
            if (version != Versions.MATCH_ANY) {
                return putParam("version", Long.toString(version));
            }
            return this;
        }

        Params withVersionType(VersionType versionType) {
            if (versionType != VersionType.INTERNAL) {
                return putParam("version_type", versionType.name().toLowerCase(Locale.ROOT));
            }
            return this;
        }

        Params withWaitForActiveShards(ActiveShardCount activeShardCount) {
            return withWaitForActiveShards(activeShardCount, ActiveShardCount.DEFAULT);
        }

        Params withWaitForActiveShards(ActiveShardCount activeShardCount, ActiveShardCount defaultActiveShardCount) {
            if (activeShardCount != null && activeShardCount != defaultActiveShardCount) {
                return putParam("wait_for_active_shards", activeShardCount.toString().toLowerCase(Locale.ROOT));
            }
            return this;
        }

        Params withIndicesOptions(IndicesOptions indicesOptions) {
            withIgnoreUnavailable(indicesOptions.ignoreUnavailable());
            putParam("allow_no_indices", Boolean.toString(indicesOptions.allowNoIndices()));
            String expandWildcards;
            if (indicesOptions.expandWildcardsOpen() == false && indicesOptions.expandWildcardsClosed() == false) {
                expandWildcards = "none";
            } else {
                StringJoiner joiner = new StringJoiner(",");
                if (indicesOptions.expandWildcardsOpen()) {
                    joiner.add("open");
                }
                if (indicesOptions.expandWildcardsClosed()) {
                    joiner.add("closed");
                }
                expandWildcards = joiner.toString();
            }
            putParam("expand_wildcards", expandWildcards);
            return this;
        }

        Params withIgnoreUnavailable(boolean ignoreUnavailable) {
            // Always explicitly place the ignore_unavailable value.
            putParam("ignore_unavailable", Boolean.toString(ignoreUnavailable));
            return this;
        }

        Params withHuman(boolean human) {
            if (human) {
                putParam("human", Boolean.toString(human));
            }
            return this;
        }

        Params withLocal(boolean local) {
            if (local) {
                putParam("local", Boolean.toString(local));
            }
            return this;
        }

        Params withIncludeDefaults(boolean includeDefaults) {
            if (includeDefaults) {
                return putParam("include_defaults", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withPreserveExisting(boolean preserveExisting) {
            if (preserveExisting) {
                return putParam("preserve_existing", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withDetailed(boolean detailed) {
            if (detailed) {
                return putParam("detailed", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withWaitForCompletion(boolean waitForCompletion) {
            if (waitForCompletion) {
                return putParam("wait_for_completion", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withNodes(String[] nodes) {
            if (nodes != null && nodes.length > 0) {
                return putParam("nodes", String.join(",", nodes));
            }
            return this;
        }

        Params withActions(String[] actions) {
            if (actions != null && actions.length > 0) {
                return putParam("actions", String.join(",", actions));
            }
            return this;
        }

        Params withTaskId(TaskId taskId) {
            if (taskId != null && taskId.isSet()) {
                return putParam("task_id", taskId.toString());
            }
            return this;
        }

        Params withParentTaskId(TaskId parentTaskId) {
            if (parentTaskId != null && parentTaskId.isSet()) {
                return putParam("parent_task_id", parentTaskId.toString());
            }
            return this;
        }

        Params withVerify(boolean verify) {
            if (verify) {
                return putParam("verify", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withWaitForStatus(ClusterHealthStatus status) {
            if (status != null) {
                return putParam("wait_for_status", status.name().toLowerCase(Locale.ROOT));
            }
            return this;
        }

        Params withWaitForNoRelocatingShards(boolean waitNoRelocatingShards) {
            if (waitNoRelocatingShards) {
                return putParam("wait_for_no_relocating_shards", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withWaitForNoInitializingShards(boolean waitNoInitShards) {
            if (waitNoInitShards) {
                return putParam("wait_for_no_initializing_shards", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withWaitForNodes(String waitForNodes) {
            return putParam("wait_for_nodes", waitForNodes);
        }

        Params withLevel(ClusterHealthRequest.Level level) {
            return putParam("level", level.name().toLowerCase(Locale.ROOT));
        }

        Params withWaitForEvents(Priority waitForEvents) {
            if (waitForEvents != null) {
                return putParam("wait_for_events", waitForEvents.name().toLowerCase(Locale.ROOT));
            }
            return this;
        }
    }

    /**
     * Ensure that the {@link IndexRequest}'s content type is supported by the Bulk API and that it conforms
     * to the current {@link BulkRequest}'s content type (if it's known at the time of this method get called).
     *
     * @return the {@link IndexRequest}'s content type
     */
    static XContentType enforceSameContentType(IndexRequest indexRequest, @Nullable XContentType xContentType) {
        XContentType requestContentType = indexRequest.getContentType();
        if (requestContentType != XContentType.JSON && requestContentType != XContentType.SMILE) {
            throw new IllegalArgumentException("Unsupported content-type found for request with content-type [" + requestContentType
                    + "], only JSON and SMILE are supported");
        }
        if (xContentType == null) {
            return requestContentType;
        }
        if (requestContentType != xContentType) {
            throw new IllegalArgumentException("Mismatching content-type found for request with content-type [" + requestContentType
                    + "], previous requests have content-type [" + xContentType + "]");
        }
        return xContentType;
    }

    /**
     * Utility class to build request's endpoint given its parts as strings
     */
    static class EndpointBuilder {

        private final StringJoiner joiner = new StringJoiner("/", "/", "");

        EndpointBuilder addPathPart(String... parts) {
            for (String part : parts) {
                if (Strings.hasLength(part)) {
                    joiner.add(encodePart(part));
                }
            }
            return this;
        }

        EndpointBuilder addCommaSeparatedPathParts(String[] parts) {
            addPathPart(String.join(",", parts));
            return this;
        }

        EndpointBuilder addPathPartAsIs(String part) {
            if (Strings.hasLength(part)) {
                joiner.add(part);
            }
            return this;
        }

        String build() {
            return joiner.toString();
        }

        private static String encodePart(String pathPart) {
            try {
                //encode each part (e.g. index, type and id) separately before merging them into the path
                //we prepend "/" to the path part to make this pate absolute, otherwise there can be issues with
                //paths that start with `-` or contain `:`
                URI uri = new URI(null, null, null, -1, "/" + pathPart, null, null);
                //manually encode any slash that each part may contain
                return uri.getRawPath().substring(1).replaceAll("/", "%2F");
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Path part [" + pathPart + "] couldn't be encoded", e);
            }
        }
    }
}
