package org.forward.entitysearch.esdocumentsearch;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchWrapperException;
import org.elasticsearch.common.io.stream.StreamInput;

import java.io.IOException;

/**
 * Generic exception implementing {@link org.elasticsearch.ElasticsearchWrapperException}
 */
@SuppressWarnings("serial")
public class ClusteringException extends ElasticsearchException implements ElasticsearchWrapperException {

    public ClusteringException(Throwable cause) {
        super(cause);
    }

    public ClusteringException(String msg, Object... args) {
        super(msg, args);
    }

    public ClusteringException(String msg, Throwable cause, Object... args) {
        super(msg, cause, args);
    }

    public ClusteringException(StreamInput in) throws IOException {
        super(in);
    }
}
