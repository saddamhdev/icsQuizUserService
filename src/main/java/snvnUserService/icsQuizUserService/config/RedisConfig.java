package snvnUserService.icsQuizUserService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import snvnUserService.icsQuizUserService.model.User;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, User> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {

        RedisSerializer<String> keySerializer = RedisSerializer.string();
        @SuppressWarnings("unchecked")
        RedisSerializer<User> valueSerializer = (RedisSerializer<User>) (RedisSerializer<?>) RedisSerializer.json();

        RedisSerializationContext<String, User> context = RedisSerializationContext
                .<String, User>newSerializationContext(SerializationPair.fromSerializer(keySerializer))
                .value(SerializationPair.fromSerializer(valueSerializer))
                .hashKey(SerializationPair.fromSerializer(keySerializer))
                .hashValue(SerializationPair.fromSerializer(valueSerializer))
                .build();

        System.out.println("✅ ReactiveRedisTemplate Bean Loaded Successfully");
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
