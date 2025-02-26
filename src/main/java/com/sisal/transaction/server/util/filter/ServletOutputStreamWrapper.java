package com.sisal.transaction.server.util.filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ServletOutputStreamWrapper extends ServletOutputStream
{
    private OutputStream outputStream;
    private ByteArrayOutputStream copy;

    public ServletOutputStreamWrapper(OutputStream outputStream)
    {
        this.outputStream = outputStream;
        this.copy = new ByteArrayOutputStream();
    }

    @Override
    public boolean isReady()
    {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener)
    {//Do nothing
    }

    public byte[] getCopy()
    {
        return copy.toByteArray();
    }

    @Override
    public void write(int b) throws IOException
    {
        outputStream.write(b);
        copy.write(b);

    }

}