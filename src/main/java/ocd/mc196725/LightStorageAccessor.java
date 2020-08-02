package ocd.mc196725;

public interface LightStorageAccessor extends ILightUpdatesHandler
{
    void disableLightUpdates(long chunkPos);

    void invokeSetColumnEnabled(long chunkPos, boolean enabled);
}
