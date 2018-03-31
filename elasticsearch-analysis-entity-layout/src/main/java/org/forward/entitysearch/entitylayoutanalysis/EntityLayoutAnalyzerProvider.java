/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.forward.entitysearch.entitylayoutanalysis;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider;
import org.elasticsearch.index.analysis.Analysis;

/**
 * @author duydo
 */
public class EntityLayoutAnalyzerProvider extends AbstractIndexAnalyzerProvider<EntityLayoutAnalyzer> {
    private final EntityLayoutAnalyzer analyzer;

    public EntityLayoutAnalyzerProvider(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
        analyzer = new EntityLayoutAnalyzer(Analysis.parseStopWords(environment, settings, EntityLayoutAnalyzer.getDefaultStopSet(), true));
    }

    @Override
    public EntityLayoutAnalyzer get() {
        return analyzer;
    }
}
