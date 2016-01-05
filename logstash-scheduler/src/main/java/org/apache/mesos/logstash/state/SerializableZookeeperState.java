package org.apache.mesos.logstash.state;

import org.apache.mesos.state.Variable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.*;
import java.security.InvalidParameterException;
import java.util.concurrent.ExecutionException;

/**
 * Writes serializable data to zookeeper
 */
@Component
public class SerializableZookeeperState implements SerializableState {
    @Inject
    private org.apache.mesos.state.State zkState;

    /**
     * Get serializable object from store
     * null if none
     *
     * @return Object
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) throws IOException{
        try {
            byte[] existingNodes = zkState.fetch(key).get().value();
            if (existingNodes.length > 0) {
                ByteArrayInputStream bis = new ByteArrayInputStream(existingNodes);
                ObjectInputStream in = null;
                try {
                    in = new ObjectInputStream(bis);
                    return (T) in.readObject();
                } finally {
                    try {
                        bis.close();
                    } finally {
                        if (in != null) {
                            in.close();
                        }
                    }
                }
            } else {
                return null;
            }
        } catch (InterruptedException | ClassNotFoundException | ExecutionException | IOException e) {
            throw new IOException("Unable to get zNode", e);
        }
    }

    /**
     * Set serializable object in store
     *
     * @throws IOException
     */
    public <T> void set(String key, T object) throws IOException {
        try {
            Variable value = zkState.fetch(key).get();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = null;
            try {
                out = new ObjectOutputStream(bos);
                out.writeObject(object);
                value = value.mutate(bos.toByteArray());
                zkState.store(value).get();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } finally {
                    bos.close();
                }
            }
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new IOException("Unable to set zNode", e);
        }
    }

    /**
     * Delete a path in zk
     * @param key the key to delete
     * @throws IOException if cannot read path
     */
    public void delete(String key) throws IOException {
        try {
            Variable value = zkState.fetch(key).get();
            if (value.value().length == 0) {
                throw new InvalidParameterException("Key does not exist:" + key);
            }
            zkState.expunge(value);
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Unable to delete key:" + key);
        }
    }
}
