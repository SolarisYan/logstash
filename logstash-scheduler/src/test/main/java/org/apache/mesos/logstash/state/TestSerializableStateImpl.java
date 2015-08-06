package org.apache.mesos.logstash.state;

import java.io.NotSerializableException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

/**
 * Dummy storage class to replace zookeeper.
 */
@SuppressWarnings("unchecked")
public class TestSerializableStateImpl implements SerializableState {
    Map<String, Object> map = new HashMap<String, Object>();

    @Override
    public <T> T get(String key) throws NotSerializableException {
        return (T) map.getOrDefault(key, null);
    }

    @Override
    public <T> void set(String key, T object) throws NotSerializableException {
        if (key.endsWith("/") && !key.equals("/")) {
            throw new NotSerializableException("Trailing slashes are not allowed");
        }
        map.put(key, object);
    }

    @Override
    public void delete(String key) throws InvalidParameterException {
        if (map.remove(key) == null) {
            throw new InvalidParameterException("Unable to delete key.");
        }
    }
}