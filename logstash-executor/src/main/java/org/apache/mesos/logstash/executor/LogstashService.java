package org.apache.mesos.logstash.executor;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.mesos.logstash.common.LogstashProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Encapsulates a logstash instance. Keeps track of the current container id for logstash.
 */
public class LogstashService {

    public static final Logger LOGGER = LoggerFactory.getLogger(LogstashService.class);

    private static String serialize(LogstashProtos.LogstashConfiguration logstashConfiguration) {
        List<LS.Plugin> inputPlugins = optionalValuesToList(
                Optional.ofNullable(logstashConfiguration.getLogstashPluginInputSyslog()).map(config -> LS.plugin("syslog", LS.map(LS.kv("port", LS.number(config.getPort()))))),
                Optional.ofNullable(logstashConfiguration.getLogstashPluginInputCollectd()).map(config -> LS.plugin("udp", LS.map(LS.kv("port", LS.number(5000 /*TODO: config.getPort()*/)), LS.kv("buffer_size", LS.number(1452)), LS.kv("codec", LS.plugin("collectd", LS.map())))))
        );

        List<LS.Plugin> filterPlugins = Arrays.asList(
            LS.plugin("mutate", LS.map(
                    LS.kv("add_field", LS.map(
                            LS.kv("mesos_slave_id", LS.string(logstashConfiguration.getMesosSlaveId()))
                        )
                    )
                )
            )
        );

        List<LS.Plugin> outputPlugins = optionalValuesToList(
                Optional.ofNullable(logstashConfiguration.getLogstashPluginOutputElasticsearch()).map(config -> LS.plugin(
                        "elasticsearch",
                        LS.map(
                                LS.kv("host", LS.string(config.getHost())),
                                LS.kv("protocol", LS.string("http")),
                                LS.kv("index", LS.string("logstash"))  //FIXME this should be configurable. Maybe add -%{+YYYY.MM.dd}
                        )
                ))
        );

        return LS.config(
                LS.section("input",  inputPlugins.toArray(new LS.Plugin[0])),
                LS.section("filter", filterPlugins.toArray(new LS.Plugin[0])),
                LS.section("output", outputPlugins.toArray(new LS.Plugin[0]))
        ).serialize();
    }

    @SafeVarargs
    private static <T> List<T> optionalValuesToList(Optional<T> ... optionals) {
        return Arrays.stream(optionals).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    public void run(LogstashProtos.LogstashConfiguration logstashConfiguration) {
        LOGGER.info("Starting the Logstash Process.");

        Process process;
        try {
            process = Runtime.getRuntime().exec(
                    new String[]{
                            "/opt/logstash/bin/logstash",
                            "--log", "/var/log/logstash.log",
                            "-e", serialize(logstashConfiguration)
                    },
                    new String[]{
                            "LS_HEAP_SIZE=" + System.getProperty("mesos.logstash.logstash.heap.size"),
                            "HOME=/root"
                    }
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to start Logstash", e);
        }

        try {
            process.waitFor();
            LOGGER.warn("Logstash quit with exit={}", process.exitValue());
        } catch (InterruptedException e) {
            throw new RuntimeException("Logstash process was interrupted", e);
        }

        try {
            IOUtils.copy(process.getErrorStream(), System.err);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read STDERR of Logstash");
        }
    }
}
