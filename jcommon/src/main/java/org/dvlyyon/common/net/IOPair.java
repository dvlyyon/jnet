package org.dvlyyon.common.net;

import java.io.Reader;
import java.io.Writer;

public interface IOPair {
    public Reader getReader();
    public Writer getWriter();
    public void reset();
    public void close();
}
