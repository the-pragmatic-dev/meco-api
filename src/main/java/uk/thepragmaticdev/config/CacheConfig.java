package uk.thepragmaticdev.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

  /**
   * Contains configuration to start a com.hazelcast.core.HazelcastInstance.
   *
   * @return A Hazelcast Config
   */
  @Bean
  public Config hazelcastConfig() {
    Config config = new Config();
    config.setInstanceName("hazelcast-instance")
        .addMapConfig(new MapConfig().setName("bucket-map")
            .setMaxSizeConfig(new MaxSizeConfig(200, MaxSizeConfig.MaxSizePolicy.FREE_HEAP_SIZE))
            .setEvictionPolicy(EvictionPolicy.LRU).setTimeToLiveSeconds(-1));
    return config;
  }

  @Bean
  public HazelcastInstance hazelcastInstance(final Config config) {
    return Hazelcast.getOrCreateHazelcastInstance(config);
  }
}
