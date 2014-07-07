import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Timer;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.mysql.jdbc.Statement;

public class FileHandler extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public static final Timer deleteTimer = new Timer();
	private static final String insertStatement = "INSERT INTO files (filename,timestamp,data) values (?,?,?)";
	private static final String getTime = "SELECT timestamp,filename from files where id = ?";
	private static final String getFile = "SELECT filename,data,length(data) from files where id = ?";
	
	private static final int DELETE_TIME = 300000;

	@Resource(name = "jdbc/dropbox")
	public static DataSource fileStore;

	public FileHandler() {
		super();
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {
			Integer key = Integer.parseInt(request.getParameter("get_file"));
			if ("true".equals(request.getParameter("get_time"))) {
				Connection con = fileStore.getConnection();
				PreparedStatement ps = con.prepareStatement(getTime);
				ps.setInt(1, key);
				ResultSet rs = ps.executeQuery();
				if(!rs.next()){
					response.sendError(404);
					return;
				}
				response.setContentType("application/json");
				JSONObject output = new JSONObject();
				output.put("time_left", (rs.getLong(1) - System.currentTimeMillis())/1000);
				output.put("filename", rs.getString(2));
				response.getWriter().write(output.toString());
				return;
			}
			Connection con = fileStore.getConnection();
			PreparedStatement ps = con.prepareStatement(getFile);
			ps.setInt(1, key);
			ResultSet rs = ps.executeQuery();
			if(!rs.next()){
				response.sendError(404);
				return;
			}
			response.setHeader(
					"Content-Disposition",
					"attachment; filename=\""
							+ rs.getString(1) + "\"");
			response.setContentLength(rs.getInt(3));
			IOUtils.copy(rs.getBinaryStream(2), response.getOutputStream());
		} catch (NumberFormatException e) {
			response.sendError(404);
		} catch (SQLException e) {
			e.printStackTrace();
			response.sendError(500);
		} catch (JSONException e) {
			e.printStackTrace();
			response.sendError(500);
		}
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		JSONObject outputObject = new JSONObject();
		try {
			List<FileItem> items = new ServletFileUpload(
					new DiskFileItemFactory()).parseRequest(request);
			for (FileItem item : items) {
				if (!item.isFormField()) {
					String filename = FilenameUtils.getName(item.getName());
					InputStream filecontent = item.getInputStream();
					if (item.getSize() > 1 * 1024 * 1024) {
						return;
					}
					Connection con = fileStore.getConnection();
					PreparedStatement ps = con
							.prepareStatement(insertStatement, Statement.RETURN_GENERATED_KEYS);
					ps.setString(1, filename);
					ps.setLong(2, System.currentTimeMillis() + DELETE_TIME);
					ps.setBinaryStream(3, filecontent,(int)item.getSize());
					ps.executeUpdate();
					ResultSet rs = ps.getGeneratedKeys();
					rs.next();
					int key = rs.getInt(1);
					outputObject.put("name", filename);
					outputObject.put("size", item.getSize());
					outputObject.put("key", key);
					deleteTimer.schedule(new FileDeleteTask(key), DELETE_TIME);
				}
			}
		} catch (FileUploadException e) {
			throw new ServletException("Cannot parse multipart request.", e);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		response.setContentType("application/json");
		response.getWriter().write(outputObject.toString());

	}

}
