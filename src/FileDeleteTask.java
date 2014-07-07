import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.TimerTask;

public class FileDeleteTask extends TimerTask {
	private Integer keyToDelete;

	private static final String deleteStatement = "DELETE FROM files where id = ?";

	public FileDeleteTask(Integer key) {
		keyToDelete = key;
	}

	public void run() {
		try {
			Connection con = FileHandler.fileStore.getConnection();
			PreparedStatement ps = con.prepareStatement(deleteStatement);
			ps.setInt(1, keyToDelete);
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
