import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;

public class ClientServiceSQL {
    private static Connection connection;
    private static PreparedStatement ps;
    private static PasswordHandler passwordHandler;
    private static String name;
    private static Long user_id;
    private static String PATH = "C:/";

    synchronized static String logIn(String str) {
        name = "";
        passwordHandler = new PasswordHandler();
        String[] parts = str.split("\\s");
        try {
            connection.setAutoCommit(false);
            ps = connection.prepareStatement("SELECT * FROM" +
                    " Users WHERE login = ?");
            ps.setString(1, parts[1]);
            ResultSet rs = ps.executeQuery();
            connection.commit();
            if (passwordHandler.validatePassword(parts[2], rs.getInt(3), rs.getString(4), rs.getString(5))) {
                user_id = rs.getLong(1);
                name = parts[1];
                ServerMain.LOGGER.info(name + " connected.");
                return "./auth ok " + parts[1];
            }
        } catch (NoSuchAlgorithmException | SQLException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return "./logIn failed";
    }

    synchronized static boolean signUp(String str) throws InvalidKeySpecException, NoSuchAlgorithmException {
        name = "";
        passwordHandler = new PasswordHandler();
        String[] parts = str.split("\\s");
        String hashString = passwordHandler.createHash(parts[2]);
        String[] param = hashString.split(":");
        try {
            connection.setAutoCommit(false);
            ps = connection.prepareStatement("INSERT INTO Users (login, iteration, salt, hash) VALUES (?,?,?,?)");
            ps.setString(1, parts[1]);
            ps.setString(2, param[0]);
            ps.setString(3, param[1]);
            ps.setString(4, param[2]);
            ps.executeUpdate();
            connection.commit();
            name = parts[1];
            setPath();
        } catch (SQLException throwable) {
            return false;
        }
        return true;
    }

    synchronized static String getPath () {
        try {
            connection.setAutoCommit(false);
            ps = connection.prepareStatement("SELECT filePath FROM Files WHERE user_ID = ?");
            ps.setLong(1, user_id);
            ResultSet rs = ps.executeQuery();
            connection.commit();
            PATH = rs.getString(1);
            return PATH;
        } catch (SQLException e) {
            e.getStackTrace();
        }
        return null;
    }

    synchronized static void setPath () {
        try {
            PATH = PATH + name + "/";
            connection.setAutoCommit(false);
            ps = connection.prepareStatement("SELECT User_ID FROM Users WHERE login = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            user_id = rs.getLong(1);
            ps = connection.prepareStatement("INSERT INTO Files (User_ID, filePath) VALUES (?,?)");
            ps.setLong(1, rs.getLong(1));
            ps.setString(2, PATH);
            ps.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            e.getStackTrace();
        }
    }

    synchronized static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:C:\\CloudStorage_Demo\\ServerStorage\\src\\main\\resources\\CloudStorage.db");
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static void disconnect() {
        try {
            connection.close();
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
    }
}
