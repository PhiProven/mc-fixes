package ocd.mc196725;

public class EmptyChunkNibbleArray extends ReadonlyChunkNibbleArray
{
    public EmptyChunkNibbleArray()
    {
    }

    @Override
    public byte[] asByteArray()
    {
        return new byte[2048];
    }
}
