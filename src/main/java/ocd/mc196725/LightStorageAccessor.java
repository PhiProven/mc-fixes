package ocd.mc196725;

public interface LightStorageAccessor
{
    /**
     * Enables or disables light updates for the provided <code>chunkPos</code>.
     * Disabling light updates additionally disables source light and removes all data associated to the chunk.
     */
    void setLightUpdatesEnabled(long chunkPos, boolean enabled);

    void invokeSetColumnEnabled(long chunkPos, boolean enabled);
}
