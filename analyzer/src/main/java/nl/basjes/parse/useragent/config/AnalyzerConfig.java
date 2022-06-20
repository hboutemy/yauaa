/*
 * Yet Another UserAgent Analyzer
 * Copyright (C) 2013-2022 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.basjes.parse.useragent.config;

import nl.basjes.parse.useragent.analyze.InvalidParserConfigurationException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static nl.basjes.parse.useragent.analyze.UserAgentStringMatchMaker.DEFAULT_USER_AGENT_MAX_LENGTH;

public final class AnalyzerConfig implements Serializable {

    // If we want ALL fields this is null. If we only want specific fields this is a list of names.
    private Set<String> wantedFieldNames = null; // NOSONAR: Only accessed via Builder.

    // file+line number --> Config at that location
    private final Map<String, MatcherConfig> matcherConfigs = new LinkedHashMap<>();
    // Lookup name --> keys+values for the lookup
    private final Map<String, Map<String, String>> lookups = new LinkedHashMap<>(128);

    // Lookup SET name --> values of this set
    private final Map<String, Set<String>> lookupSets = new LinkedHashMap<>(128);

    private int userAgentMaxLength = -1;

    private final List<TestCase> testCases = new ArrayList<>(8192);

    private AnalyzerConfig() {
    }

    public static AnalyzerConfigBuilder newBuilder() {
        return new AnalyzerConfigBuilder();
    }

    public void merge(AnalyzerConfig additionalConfig) {
        testCases       .addAll(additionalConfig.testCases);
        lookups         .putAll(additionalConfig.lookups);
        lookupSets      .putAll(additionalConfig.lookupSets);
        matcherConfigs  .putAll(additionalConfig.matcherConfigs);
        if (additionalConfig.userAgentMaxLength >= 0) {
            userAgentMaxLength = Math.min(userAgentMaxLength, additionalConfig.userAgentMaxLength);
        }
        if (additionalConfig.wantedFieldNames == null) {
            wantedFieldNames = null;
        } else {
            if (wantedFieldNames == null) {
                wantedFieldNames = new TreeSet<>(additionalConfig.wantedFieldNames);
            } else {
                wantedFieldNames.addAll(additionalConfig.wantedFieldNames);
            }
        }
    }

    public Set<String> getWantedFieldNames() {
        return wantedFieldNames;
    }

    public Map<String, MatcherConfig> getMatcherConfigs() {
        return matcherConfigs;
    }

    public Map<String, Map<String, String>> getLookups() {
        return lookups;
    }

    public Map<String, Set<String>> getLookupSets() {
        return lookupSets;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public int getUserAgentMaxLength() {
        return userAgentMaxLength;
    }

    public AnalyzerConfig setUserAgentMaxLength(int newUserAgentMaxLength) {
        if (newUserAgentMaxLength < 0) {
            this.userAgentMaxLength = DEFAULT_USER_AGENT_MAX_LENGTH;
        } else {
            this.userAgentMaxLength = newUserAgentMaxLength;
        }
        return this;
    }

    public AnalyzerConfig wantedFieldNames(Set<String> newWantedFieldNames) {
        if (newWantedFieldNames == null || newWantedFieldNames.isEmpty()) {
            this.wantedFieldNames = null;
        } else {
            this.wantedFieldNames = new TreeSet<>(newWantedFieldNames);
        }
        return this;
    }

    public static class AnalyzerConfigBuilder {

        // Lookup name --> names of lookups to merge in it
        private final Map<String, Set<String>> lookupMerge = new LinkedHashMap<>(128); // The names of the lookups that need to be merged

        // Lookup SET name --> names of lookup SETs to merge in it
        private final Map<String, Set<String>> lookupSetMerge = new LinkedHashMap<>(128);  // The names of the sets that need to be merged

        private final AnalyzerConfig analyzerConfig;

        public AnalyzerConfigBuilder() {
            this.analyzerConfig = new AnalyzerConfig();
        }

        public void addMatcherConfigs(String filename, MatcherConfig matcherConfig) {
            analyzerConfig.matcherConfigs.put(filename, matcherConfig);
        }

        /**
         * Store the keys and values.
         *
         * @param name   The name of the lookup
         * @param values The additional keys and values for this lookup.
         */
        public AnalyzerConfigBuilder putLookup(String name, Map<String, String> values) {
            analyzerConfig.lookups.put(name, values);
            return this;
        }

        /**
         * @param newLookups The additional lookups.
         */
        public AnalyzerConfigBuilder putLookups(Map<String, Map<String, String>> newLookups) {
            // FIXME: Handle lookupnames that already exist
            analyzerConfig.lookups.putAll(newLookups);
            return this;
        }

        /**
         * Store the additional lookups that need to be merged in with a lookup
         * @param name The name of the lookup
         * @param lookupNames The names of the lookups that must be added to the specified lookup.
         */
        public AnalyzerConfigBuilder putLookupMerges(String name, Set<String> lookupNames) {
            lookupMerge.put(name, lookupNames);
            return this;
        }

        /**
         * Store the keys and values.
         * @param name   The name of the lookupSet
         * @param values The additional keys and values for this lookup.
         */
        public AnalyzerConfigBuilder putLookupSets(String name, Set<String> values) {
            analyzerConfig.lookupSets.put(name, values);
            return this;
        }

        /**
         * @param newLookupSets The additional lookup sets.
         */
        public AnalyzerConfigBuilder putLookupSets(Map<String, Set<String>> newLookupSets) {
            // FIXME: Handle lookupnames that already exist
            analyzerConfig.lookupSets.putAll(newLookupSets);
            return this;
        }

        /**
         * Store the additional lookupSets that need to be merged in with a lookupSet
         * @param name The name of the lookupSet
         * @param setNames The names of the lookupSets that must be added to the specified lookupSet.
         */
        public AnalyzerConfigBuilder putLookupSetsMerges(String name, Set<String> setNames) {
            lookupSetMerge.put(name, setNames);
            return this;
        }

        public AnalyzerConfigBuilder clearAllTestCases() {
            analyzerConfig.testCases.clear();
            return this;
        }

        public AnalyzerConfigBuilder addTestCase(TestCase testCase) {
            analyzerConfig.testCases.add(testCase);
            return this;
        }

        public AnalyzerConfigBuilder withUserAgentMaxLength(int userAgentMaxLength) {
            analyzerConfig.userAgentMaxLength = userAgentMaxLength;
            return this;
        }

        public AnalyzerConfig build() {
            if (!analyzerConfig.lookups.isEmpty()) {
                if (!lookupMerge.isEmpty()) {
                    lookupMerge.forEach((mapName, allExtraToLoad) -> {
                        Map<String, String> theMap = analyzerConfig.lookups.get(mapName);
                        if (theMap != null) {
                            allExtraToLoad.forEach(extraToLoad -> {
                                Map<String, String> extraMap = analyzerConfig.lookups.get(extraToLoad);
                                if (extraMap == null) {
                                    throw new InvalidParserConfigurationException("Unable to merge lookup '" + extraToLoad + "' into '" + mapName + "'.");
                                }
                                theMap.putAll(extraMap);
                            });
                        }
                    });
                }

                // All compares are done in a case insensitive way. So we lowercase ALL keys of the lookups beforehand.
                Map<String, Map<String, String>> cleanedLookups = new LinkedHashMap<>(analyzerConfig.lookups.size());
                for (Map.Entry<String, Map<String, String>> lookupsEntry : analyzerConfig.lookups.entrySet()) {
                    Map<String, String> cleanedLookup = new LinkedHashMap<>(lookupsEntry.getValue().size());
                    for (Map.Entry<String, String> entry : lookupsEntry.getValue().entrySet()) {
                        cleanedLookup.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
                    }
                    cleanedLookups.put(lookupsEntry.getKey(), cleanedLookup);
                }
                analyzerConfig.lookups.clear();
                analyzerConfig.lookups.putAll(cleanedLookups);
            }

            if (!lookupSetMerge.isEmpty()) {
                lookupSetMerge.forEach((setName, allExtraToLoad) -> {
                    Set<String> theSet = analyzerConfig.lookupSets.get(setName);
                    if (theSet != null) {
                        allExtraToLoad.forEach(extraToLoad -> {
                            Map<String, String> extralookup = analyzerConfig.lookups.get(extraToLoad);
                            if (extralookup != null) {
                                theSet.addAll(extralookup.keySet());
                            }
                            Set<String> extralookupSet = analyzerConfig.lookupSets.get(extraToLoad);
                            if (extralookupSet != null) {
                                theSet.addAll(extralookupSet);
                            }
                            if (extralookup == null && extralookupSet == null) {
                                throw new InvalidParserConfigurationException("Unable to merge set '" + extraToLoad + "' into '" + setName + "'.");
                            }

                        });
                    }
                });
            }
            return analyzerConfig;
        }
    }

    @Override
    public String toString() {
        return "AnalyzerConfig {\n" +
            "   matcherConfigs=" + matcherConfigs + ",\n" +
            "   lookups=" + lookups.size() + ",\n" +
            "   lookupSets=" + lookupSets.size() + ",\n" +
            "   testCases=" + testCases.size()  + ",\n" +
            "   userAgentMaxLength=" + userAgentMaxLength + ",\n" +
            "\n}";
    }
}