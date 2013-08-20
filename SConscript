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
        env.File('$BUILD_DIR/log_search.jar'),
        env.File('$EXTLIB/tools.cli-0.2.2.jar'),
        env.File('$EXTLIB/serializable-fn-0.0.3.jar'),        
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
    env.File('$BUILD_DIR/argparser.jar'),
    env.File('$BUILD_DIR/log_analyzer_consumer.jar'),
    env.File('$BUILD_DIR/zktools.jar'),
    env.File('$BUILD_DIR/kfktools.jar'),
    env.File('$BUILD_DIR/logging.jar')
    ],
    standalone=True, manifest={'Main-Class': 'web.main'}))
env.install(env.compileAndJar('log_analyzer_consumer.jar', 'log_consumer',
    libs=[env['CLOJURE'],
    env.File('$BUILD_DIR/utilities.jar'),
    env.File('$EXTLIB/data.json-0.2.2.jar'),
    env.File('$BUILD_DIR/zktools.jar'),
    env.File('$BUILD_DIR/kfktools.jar'),
    env.File('$BUILD_DIR/logging.jar'),
    ]))
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
env.install(env.compileAndJar('log_search.jar', 'log_search',
    libs=[env['CLOJURE'],
    env.File('$EXTLIB/ring-1.1.8.jar'),
    env.File('$EXTLIB/ring-jetty-adapter-1.1.8.jar'),
    env.File('$EXTLIB/compojure-1.1.5.jar'),
    env.File('$BUILD_DIR/zktools.jar'),
    env.File('$BUILD_DIR/kfktools.jar'),
    env.File('$BUILD_DIR/argparser.jar'),
    env.File('$EXTLIB/data.json-0.2.2.jar'),
    env.File('$EXTLIB/serializable-fn-0.0.3.jar'),
    env.File('$BUILD_DIR/logging.jar')    
    ],
    install={
        env.File('$BUILD_DIR/front/log_monitor.js'): '@/resources/js',
        env.File('$EXTLIB/highcharts.js'): '@/resources/js',
        env.File('$EXTLIB/jquery-1.10.2.min.js'): '@/resources/js',
        env.File('#front/resources/js/jquery-ui.js'): '@/resources/js',
        env.File('#front/resources/js/jquery.loadmask.min.js'): '@/resources/js',
        env.File('#front/resources/js/bootstrap.min.js'): '@/resources/js',
        env.File('#front/resources/js/jquery.pagination.js'): '@/resources/js',
        env.File('#front/log_monitor/index.html'): '@/resources/',
        env.File('#front/resources/css/bootstrap.css'): '@/resources/css',
        env.File('#front/resources/css/index.css'): '@/resources/css',
        env.File('#front/resources/css/jquery-ui.css'): '@/resources/css',
        env.File('#front/resources/css/jquery.loadmask.css'): '@/resources/css',
        env.File('#front/resources/image/bar.png'): '@/resources/image',
        env.File('#front/resources/image/go_btn.png'): '@/resources/image',
        env.File('#front/resources/image/icons_sprite.png'): '@/resources/image',
        env.File('#front/resources/image/loader.gif'): '@/resources/image',
        env.File('#front/resources/image/logo.png'): '@/resources/image',
        env.File('#front/resources/image/search_bar.png'): '@/resources/image',
        env.File('#front/resources/image/shadow_soft.png'): '@/resources/image',
        env.File('#front/resources/image/splIcons.gif'): '@/resources/image',
        env.File('#front/resources/image/sprite_button_icons.png'): '@/resources/image',
    },
))

env.install(env.compileAndJar('spark_demo.jar', 'spark_demo',
    libs=[env['CLOJURE'],
    env.File('$EXTLIB/spark-core_2.9.3-0.7.3.jar'),
    env.File('$BUILD_DIR/log_search.jar'),
    env.File('$BUILD_DIR/clj_spark_rebuild.jar'),
    ]))
env.install(env.compileAndJar('clj_spark_rebuild.jar', 'clj_spark',
    libs=[env['CLOJURE'],
    env.File('$EXTLIB/spark-core_2.9.3-0.7.3.jar')
    ],
    standalone=True, manifest={'Main-Class': 'clj_spark.api'}))
