Import('env')

env.install(env.compileAndJar('log_collector.jar', 'log_collector',
    libs=[env['CLOJURE'],
            env.File('$BUILD_DIR/utilities.jar')
        ]))
env.install(env.compileAndJar('log_analyzer_unittest.jar', 'unittest',
    libs=[env['CLOJURE'], 
        env.File('$BUILD_DIR/log_collector.jar'),
        env.File('$EXTLIB/tools.cli-0.2.2.jar'),
        env.File('$BUILD_DIR/testing.jar')],
    standalone=True, manifest={'Main-Class': 'unittest.main'}))
env.install(env.compileAndJar('log_analyzer_smoketest.jar', 'smoketest',
    libs=[env['CLOJURE'], 
        env.File('$BUILD_DIR/log_collector.jar'),
        env.File('$EXTLIB/tools.cli-0.2.2.jar'),
        env.File('$BUILD_DIR/testing.jar'),
        env.File('$BUILD_DIR/utilities.jar')],
    standalone=True, manifest={'Main-Class': 'smoketest.main'}))
env.install(env.compileAndJar('log_analyzer_web.jar', 'web',
    libs=[env['CLOJURE'],
    env.File('$EXTLIB/ring-1.1.8.jar'),
    env.File('$EXTLIB/ring-jetty-adapter-1.1.8.jar'),
    env.File('$BUILD_DIR/log_analyzer_consumer.jar')    
    ],
    standalone=True, manifest={'Main-Class': 'web.main'}))
env.install(env.compileAndJar('log_analyzer_consumer.jar', 'log_consumer',
    libs=[env['CLOJURE']],
    standalone=True, manifest={'Main-Class': 'log_consumer.main'}))
env.install(env.compileAndJar('log_analyzer_hadoop_adapt.jar', 'hadoop_adapt',
    libs=[env['CLOJURE'],
    env.File('$EXTLIB/hadoop-core-1.1.2.jar'),
    env.File('$EXTLIB/lib-for-hadoop/jackson-mapper-asl-1.8.8.jar'),
    env.File('$EXTLIB/lib-for-hadoop/jackson-core-asl-1.8.8.jar'),
    env.File('$EXTLIB/lib-for-hadoop/commons-logging-1.1.1.jar'),
    env.File('$EXTLIB/lib-for-hadoop/commons-configuration-1.6.jar'),
    env.File('$EXTLIB/lib-for-hadoop/commons-lang-2.4.jar')
    ],
    standalone=True, manifest={'Main-Class': 'hadoop_adapt.jobtracker'}))