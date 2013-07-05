Import('env')

env.install(env.compileAndJar('log_collector.jar', 'log_collector',
    libs=[env['CLOJURE'],
            env.File('$EXTLIB/data.json-0.2.2.jar'),
            env.File('$BUILD_DIR/utilities.jar'),
            env.File('$BUILD_DIR/kfktools.jar'),
            env.File('$BUILD_DIR/argparser.jar'),
            env.File('$BUILD_DIR/logging.jar'),
        ],
    standalone=True, manifest={'Main-Class': 'log_collector.main'}))
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
        env.File('$BUILD_DIR/utilities.jar'),
        env.File('$EXTLIB/data.json-0.2.2.jar'),
        env.File('$BUILD_DIR/zktools.jar'),
        env.File('$BUILD_DIR/kfktools.jar'),
    ],
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
