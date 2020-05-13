package uk.thepragmaticdev.security.request;

import com.maxmind.geoip2.DatabaseReader;
import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface DatabaseReaderFactory {

  DatabaseReader create(Path path) throws IOException;
}