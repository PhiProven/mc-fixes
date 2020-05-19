package ocd.mc170010;

public interface InitialLightingAccessor
{
    void forceloadLightmap(long pos);

    void unloadForcedLightmap(long pos);
}
