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
package de.eleon.watchcopy.agent;

import de.eleon.watchcopy.*;
import org.springsource.loaded.agent.ClassPreProcessorAgentAdapter;
import org.springsource.loaded.agent.SpringLoadedPreProcessor;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.List;

/**
 * JVM Agent to init WatchCopy inside a running JVM
 */
public class WatchCopyAgent {

    protected static Instrumentation instrumentation;
    protected static WatchCopy watchCopy;

    private static ClassFileTransformer transformer = new ClassPreProcessorAgentAdapter();

    public static void premain(String args, Instrumentation instrumentation) throws Exception {
        agentmain(args, instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        if (WatchCopyAgent.instrumentation != null) {
            return;
        }
        WatchCopyAgent.instrumentation = instrumentation;

        startWatchCopy();

        WatchCopyAgent.instrumentation.addTransformer(transformer);
        SpringLoadedPreProcessor.registerGlobalPlugin(new ReloadObserver());

    }

    private static void startWatchCopy() {
        Log.LOG("init");
        try {
            List<Config> configs = Configs.getConfigsFromSystemProperties("watchcopy");
            watchCopy = new WatchCopy(configs);
            watchCopy.run(true);
        } catch (IllegalArgumentException e) {
            throw new UnsupportedOperationException("\nJVM was not started with \n" +
                    "<pathTo>/watchcopy-agent.jar \\\n" +
                    "\t -Dwatchcopy.from[0]=<yourBuildDirectory> \\\n" +
                    "\t -Dwatchcopy.to[0]=<yourClasspathDirectory>\n", e);
        } catch (IOException e) {
            throw new UnsupportedOperationException("\nJVM was not started with \n" +
                    "<pathTo>/watchcopy-agent.jar \\\n" +
                    "\t -Dwatchcopy.from[0]=<yourBuildDirectory> \\\n" +
                    "\t -Dwatchcopy.to[0]=<yourClasspathDirectory>\n", e);
        }
    }

    public static Instrumentation getInstrumentation() {
        if (instrumentation == null) {
            throw new UnsupportedOperationException("Java was not started with preMain -javaagent for WatchCopy");
        }
        return instrumentation;
    }
}
