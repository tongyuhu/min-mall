package com.minimall.app;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;

class ModuleBoundaryTest {
  @Test
  void businessModulesDoNotImportOtherModulesPersistence() throws Exception {
    Path root = Path.of("").toAbsolutePath();
    if (!Files.isDirectory(root.resolve("mall-common"))) root = root.getParent();
    Pattern imports = Pattern.compile("import\\s+com\\.minimall\\.(\\w+)\\.[^;]*\\.persistence\\.");
    for (String module : List.of("user", "catalog", "marketing", "trade", "admin", "app")) {
      try (var files = Files.walk(root.resolve("mall-" + module + "/src/main/java"))) {
        for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
          var matcher = imports.matcher(Files.readString(file));
          while (matcher.find())
            assertTrue(
                module.equals(matcher.group(1)) || "common".equals(matcher.group(1)),
                file.toString());
        }
      }
    }
  }
}
