package net.tuis.ubench;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

class InlineFormatter extends Formatter {

    private static final ThreadLocal<CharArrayWriter> chars = ThreadLocal.withInitial(CharArrayWriter::new);

    private static final class TraceWriter extends PrintWriter {
        public TraceWriter() {
            super(chars.get());
        }
    }

    private static final ThreadLocal<TraceWriter> writers = ThreadLocal.withInitial(TraceWriter::new);

    @Override
    public String format(LogRecord log) {
        String msg = formatMessage(log);
        return String.format("%-6s %tF %<tT.%<tL %s(%s): %s\n%s",
                log.getLevel(), log.getMillis(), log.getSourceClassName(), log.getSourceMethodName(), msg,
                dumpStack(log.getThrown()));
    }

    private String dumpStack(final Throwable thrown) {
        if (thrown == null) {
            return "";
        }
        try (PrintWriter pw = writers.get()) {
            thrown.printStackTrace(pw);
            pw.flush();
            String data = new String(chars.get().toString());
            chars.get().reset();
            return data;
        }
    }

}
