package net.tuis.ubench;

import java.io.BufferedWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * This writer will pass all requests through to the wrapped parent System.out.
 * close() on this instance will instead just flush().
 * 
 * @author rolf
 *
 */
class NonClosingSystemOut extends FilterWriter {

    public NonClosingSystemOut() {
        super(new BufferedWriter(new OutputStreamWriter(System.out)));
    }

    @Override
    public void close() throws IOException {
        super.flush();
    }

}
