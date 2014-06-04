/*
* Copyright 2014 Dominik Foerderreuther <dominik@eleon.de>
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package de.eleon.watchcopy;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Properties;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ConfigsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldGetConfig() {
        Properties properties = new Properties();
        properties.setProperty("watchcopy.from[0]", "/tmp/from1");
        properties.setProperty("watchcopy.to[0]", "/tmp/to1");

        properties.setProperty("watchcopy.from[1]", "/tmp/from2");
        properties.setProperty("watchcopy.to[1]", "/tmp/to2");

        List<Config> configs = Configs.getConfigsFromPoperties("watchcopy", properties);

        assertThat(configs.size(), is(2));

        Optional<Config> config0 = byFrom(configs, "/tmp/from1");
        assertTrue(config0.isPresent());
        assertThat(config0.get().getTo().toString(), is("/tmp/to1"));


        Optional<Config> config1 = byFrom(configs, "/tmp/from2");
        assertTrue(config1.isPresent());
        assertThat(config1.get().getTo().toString(), is("/tmp/to2"));

    }

    @Test
    public void shouldGetErrorWhenEmpty() {
        Properties properties = new Properties();

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("need params -Dwatchcopy.from[0]=value -Dwatchcopy.to[0]=value");
        List<Config> configs = Configs.getConfigsFromPoperties("watchcopy", properties);
    }

    @Test
    public void shouldGetErrorWhenIncomplete() {
        Properties properties = new Properties();
        properties.setProperty("watchcopy.from[0]", "/tmp/from1");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("need param -Dwatchcopy.to[0]=value");
        List<Config> configs = Configs.getConfigsFromPoperties("watchcopy", properties);
    }

    @Test
    public void shouldGetErrorWhenParamEmpty() {
        Properties properties = new Properties();
        properties.setProperty("watchcopy.from[0]", "");
        properties.setProperty("watchcopy.from[1]", "");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("need param -Dwatchcopy.from[0]=value");
        List<Config> configs = Configs.getConfigsFromPoperties("watchcopy", properties);
    }

    @Test
    public void shouldGetFromSystemProperties() {
        System.setProperty("watchcopy.from[0]", "/tmp/sys/from1");
        System.setProperty("watchcopy.to[0]", "/tmp/sys/to1");

        List<Config> configs = Configs.getConfigsFromSystemProperties("watchcopy");

        Optional<Config> config0 = byFrom(configs, "/tmp/sys/from1");
        assertTrue(config0.isPresent());
        assertThat(config0.get().getTo().toString(), is("/tmp/sys/to1"));
    }

    private Optional<Config> byFrom(List<Config> configs, final String from) {
        return FluentIterable.from(configs).firstMatch(new Predicate<Config>() {
            @Override
            public boolean apply(Config config) {
                return config.getFrom().toString().equals(from);
            }
        });
    }

}