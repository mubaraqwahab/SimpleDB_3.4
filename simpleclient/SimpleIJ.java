import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import simpledb.jdbc.embedded.EmbeddedDriver;
import simpledb.jdbc.network.NetworkDriver;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.EndOfFileException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class SimpleIJ {
   public static void main(String[] args) {
      String connectionUri;
      if (args.length == 0) {
         Scanner sc = new Scanner(System.in);
         System.out.println("Connect> ");
         connectionUri = sc.nextLine().trim();
         sc.close();
      } else {
         connectionUri = args[0];
      }

      Driver d = (connectionUri.contains("//")) ? new NetworkDriver() : new EmbeddedDriver();

      try (Terminal terminal = TerminalBuilder.builder().system(true).build();
            Connection conn = d.connect(connectionUri, null);
            Statement stmt = conn.createStatement()) {
         LineReader reader = LineReaderBuilder.builder()
               .terminal(terminal)
               .history(new DefaultHistory())
               .build();

         String defaultPrompt = "SQL=> ";
         String continuationPrompt = "SQL-> ";

         List<String> cmdLines = new ArrayList<String>();

         while (true) {
            String prompt = cmdLines.size() == 0 ? defaultPrompt : continuationPrompt;
            try {
               String line = reader.readLine(prompt).trim().toLowerCase();
               cmdLines.add(line);

               String cmd = String.join(" ", cmdLines).trim();

               if (cmd.isEmpty()) {
                  cmdLines.clear();
               } else if (cmd.equals("exit") || cmd.equals("exit;")) {
                  break;
               } else if (cmd.endsWith(";")) {
                  String sqlCmd = cmd.substring(0, cmd.length() - 1);
                  if (sqlCmd.startsWith("select")) {
                     doQuery(stmt, sqlCmd);
                  } else {
                     doUpdate(stmt, sqlCmd);
                  }
                  cmdLines.clear();
               }
            } catch (UserInterruptException e) {
               System.out.println("Interrupted. Exiting.");
               break;
            } catch (EndOfFileException e) {
               System.out.println("EOF. Exiting.");
               break;
            }
         }
      } catch (IOException e) {
         System.err.println("Error initializing SimpleIJ: " + e.getMessage());
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   private static void doQuery(Statement stmt, String cmd) {
      try (ResultSet rs = stmt.executeQuery(cmd)) {
         ResultSetMetaData md = rs.getMetaData();
         int numcols = md.getColumnCount();
         int totalwidth = 0;

         // print header
         for (int i = 1; i <= numcols; i++) {
            String fldname = md.getColumnName(i);
            int width = md.getColumnDisplaySize(i);
            totalwidth += width;
            String fmt = "%" + width + "s";
            System.out.format(fmt, fldname);
         }
         System.out.println();
         for (int i = 0; i < totalwidth; i++)
            System.out.print("-");
         System.out.println();

         // print records
         while (rs.next()) {
            for (int i = 1; i <= numcols; i++) {
               String fldname = md.getColumnName(i);
               int fldtype = md.getColumnType(i);
               String fmt = "%" + md.getColumnDisplaySize(i);
               if (fldtype == Types.INTEGER) {
                  int ival = rs.getInt(fldname);
                  System.out.format(fmt + "d", ival);
               } else {
                  String sval = rs.getString(fldname);
                  System.out.format(fmt + "s", sval);
               }
            }
            System.out.println();
         }
      } catch (SQLException e) {
         System.out.println("SQL Exception: " + e.getMessage());
      }
   }

   private static void doUpdate(Statement stmt, String cmd) {
      try {
         int howmany = stmt.executeUpdate(cmd);
         System.out.println(howmany + " records processed");
      } catch (SQLException e) {
         System.out.println("SQL Exception: " + e.getMessage());
      }
   }
}