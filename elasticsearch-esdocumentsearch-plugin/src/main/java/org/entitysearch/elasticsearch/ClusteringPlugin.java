package org.forward.entitysearch.esdocumentsearch;

import org.forward.entitysearch.esdocumentsearch.ClusteringAction.TransportClusteringAction;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.lang.String;

import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.script.AbstractDoubleSearchScript;
import org.elasticsearch.script.ExecutableScript;

import java.util.Map;
import org.elasticsearch.common.Nullable;
import java.util.function.Function;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.elasticsearch.script.ScriptEngineService;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Scorer;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.LeafSearchScript;
// import org.elasticsearch.script.ScriptEngine;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.lookup.SearchLookup;


/** */
public class ClusteringPlugin extends Plugin implements ActionPlugin, ScriptEngineService {
    /**
     * Master on/off switch property for the plugin (general settings).
     */
    public static final String DEFAULT_ENABLED_PROPERTY_NAME = "carrot2.enabled";

    /**
     * Plugin name.
     */
    public static final String PLUGIN_NAME = "elasticsearch-esdocumentsearch";

    /**
     * A property key holding
     * the default component suite's resource name.
     */
    public static final String DEFAULT_SUITE_PROPERTY_NAME = "suite";

    /**
     * A property key holding
     * the default location of additional resources (stopwords, etc.) for
     * algorithms. The location is resolved relative to <code>es/conf</code>
     * but can be absolute. By default it is <code>.</code>.
     */
    public static final String DEFAULT_RESOURCES_PROPERTY_NAME = "resources";

    /**
     * A property key with the size
     * of the clustering controller's algorithm pool. By default the size
     * is zero, meaning the pool is sized dynamically. You can specify a fixed
     * number of component instances to limit resource usage.
     */
    public static final String DEFAULT_COMPONENT_SIZE_PROPERTY_NAME = "controller.pool-size";

    private final boolean transportClient;
    private final boolean pluginEnabled;

    public ClusteringPlugin(Settings settings) {
        this.pluginEnabled = settings.getAsBoolean(DEFAULT_ENABLED_PROPERTY_NAME, true);
        this.transportClient = TransportClient.CLIENT_TYPE.equals(Client.CLIENT_TYPE_SETTING_S.get(settings));
    }

/*
    // Long: For Native Script which is used for custom ranking

    @Override
    public List<NativeScriptFactory> getNativeScripts() {
        return Collections.singletonList(new MyNativeScriptFactory());
    }

    public static class MyNativeScriptFactory implements NativeScriptFactory {
        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) {
            return new MyNativeScript();
        }
        @Override
        public boolean needsScores() {
            return false;
        }
        @Override
        public String getName() {
            return "my_script";
        }
    }

    public static class MyNativeScript extends AbstractDoubleSearchScript {
        @Override
        public double runAsDouble() {
            double a = (double) source().get("a");
            double b = (double) source().get("b");
            return a * b;
        }
    }

    // End Long: For Native Script which is used for custom ranking
*/

    // Begin Long Script Engine Service
    @Override
    public String getType() {
        return "expert_scripts";
    }

    @Override
    public Function<Map<String,Object>,SearchScript> compile(String scriptName, String scriptSource, Map<String, String> params) {
        // we use the script "source" as the script identifier
        if ("pure_df".equals(scriptSource)) {
            return p -> new SearchScript() {
                final String field;
                final String term;
                {
                    if (p.containsKey("field") == false) {
                        throw new IllegalArgumentException("Missing parameter [field]");
                    }
                    if (p.containsKey("term") == false) {
                        throw new IllegalArgumentException("Missing parameter [term]");
                    }
                    field = p.get("field").toString();
                    term = p.get("term").toString();
                }

                @Override
                public LeafSearchScript getLeafSearchScript(LeafReaderContext context) throws IOException {
                    PostingsEnum postings = context.reader().postings(new Term(field, term));
                    if (postings == null) {
                        // the field and/or term don't exist in this segment, so always return 0
                        return () -> 0.0d;
                    }
                    return new LeafSearchScript() {
                        int currentDocid = -1;
                        @Override
                        public void setDocument(int docid) {
                            // advance has undefined behavior calling with a docid <= its current docid
                            if (postings.docID() < docid) {
                                try {
                                    postings.advance(docid);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            }
                            currentDocid = docid;
                        }
                        @Override
                        public double runAsDouble() {
                            if (postings.docID() != currentDocid) {
                                // advance moved past the current doc, so this doc has no occurrences of the term
                                return 0.0d;
                            }
                            try {
                                return postings.freq();
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    };
                }

                @Override
                public boolean needsScores() {
                    return false;
                }
            };
        }
        throw new IllegalArgumentException("Unknown script name " + scriptSource);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SearchScript search(CompiledScript compiledScript, SearchLookup lookup, @Nullable Map<String, Object> params) {
      Function<Map<String,Object>,SearchScript> scriptFactory = (Function<Map<String,Object>,SearchScript>) compiledScript.compiled();
      return scriptFactory.apply(params);
    }

    @Override
    public ExecutableScript executable(CompiledScript compiledScript, @Nullable Map<String, Object> params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInlineScriptEnabled() {
        return true;
    }

    @Override
    public void close() {}
    // End Script Engine Service

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        if (pluginEnabled) {
            return Arrays.asList(
                    new ActionHandler<>(ClusteringAction.INSTANCE, TransportClusteringAction.class)
                    );
        }
        return Collections.emptyList();
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings, RestController restController,
      ClusterSettings clusterSettings, IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter,
      IndexNameExpressionResolver indexNameExpressionResolver, Supplier<DiscoveryNodes> nodesInCluster) {
    return Arrays.asList(
        new ClusteringAction.RestClusteringAction(settings, restController));
    }
    
}
