package db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.TicketMasterAPI;

public class MySQLConnection implements DBConnection {
	//create connection with database
	private Connection conn;
	public MySQLConnection() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
			conn = DriverManager.getConnection(MySQLDBUtil.URL);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void close() {
		// TODO Auto-generated method stub
		if (conn != null) {
			try {
				conn.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/*----------------------------------------------------------------------------------------------------------*/
	
	/*
	 * setFavoriteItem
	 */
	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		// TODO Auto-generated method stub
		if (conn == null) {
			System.err.println("DB connection failed");
			return;
		}
//NSERT IGNORE INTO history(user_id, item_id) VALUES (123, ?)
		//INSERT IGNORE INTO history(user_id, item_id) VALUES (123, vv17fZ4PGklA-kMb);
		try {
//			String sql = "INSERT IGNORE INTO history(user_id, item_id) VALUES ('123', 'vv17fZ4PGklA-kMb')";
//			PreparedStatement ps = conn.prepareStatement(sql);
//			ps.execute();
			System.out.println("check string");
			System.out.println(itemIds.get(0) instanceof String);
			
			String sql = "INSERT IGNORE INTO history(user_id, item_id) VALUES (?, ?)";
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, userId);
			for (String itemId : itemIds) {
				ps.setString(2, itemId);
				ps.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		// TODO Auto-generated method stub
		if (conn == null) {
			System.err.println("DB connection failed");
			return;
		}
		try {
			String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ? ";
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, userId);
			//delete multiple categories 
			for (String itemId : itemIds) {
				ps.setString(2, itemId);
				ps.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*----------------------------------------------------------------------------------------------------------*/
	
	/*
	 *getFavoriteItem - show users all the items she favorited 
	 *the following two methods are helper function
	 */
	@Override
	public Set<Item> getFavoriteItems(String userId) {
		// TODO Auto-generated method stub
		if (conn == null) {
			return new HashSet<>();
		}
		Set<Item> favoriteItems = new HashSet<>();
		Set<String> itemIds = getFavoriteItemIds(userId);
		
		try {
			String sql = "SELECT * FROM items WHERE item_id = ?";
			PreparedStatement ps = conn.prepareStatement(sql);
			for (String itemId : itemIds) {
				ps.setString(1, itemId);
				
				ResultSet rs = ps.executeQuery();
				
				ItemBuilder builder = new ItemBuilder();
				while (rs.next()) {
					builder.setItemId(rs.getString("item_id"));
					builder.setName(rs.getString("name"));
					builder.setAddress(rs.getString("address"));
					builder.setImageUrl(rs.getString("image_url"));
					builder.setUrl(rs.getString("url"));
					builder.setCategories(getCategories(itemId));
					builder.setDistance(rs.getDouble("distance"));
					builder.setRating(rs.getDouble("rating"));
					
					favoriteItems.add(builder.build());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return favoriteItems;
	}
	//helper
	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		// TODO Auto-generated method stub
		if (conn == null) {
			return new HashSet<>();
		}
		Set<String> itemIds = new HashSet<>();
		try {
			String sql = "SELECT item_id FROM history WHERE user_id = ?";
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, userId);
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				String itemId = rs.getString("item_id");
				itemIds.add(itemId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return itemIds;
	}
	//helper
	@Override
	public Set<String> getCategories(String itemId) {
		// TODO Auto-generated method stub
		if (conn == null) {
			return new HashSet<>();
		}
		Set<String> categories = new HashSet<>();
		try {
			String sql = "SELECT category FROM categories WHERE item_Id = ?";
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, itemId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String category = rs.getString("category");
				categories.add(category);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return categories;
	}
	
	/*----------------------------------------------------------------------------------------------------------*/
	
	/*
	 * For searchItem servlet
	 */
	
	@Override
	public List<Item> searchItems(double lat, double lon, String term) {
		// TODO Auto-generated method stub
		TicketMasterAPI api = new TicketMasterAPI();
		List<Item> items = api.search(lat, lon, null);
		
		for (Item item : items) {
			//save data to database at the same time
			saveItem(item);
		}
		return items;
	}
	
	//For searchItem servlet
	@Override
	public void saveItem(Item item) {
		// TODO Auto-generated method stub
		if (conn == null) {
			System.err.println("DB connection failed");
			return;
		}
		try {
			String sql = "INSERT IGNORE INTO Items VALUES (?, ?, ?, ?, ?, ?, ?)";
			//An object that represents a pre-compiled SQL statement.
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, item.getItemId());
			ps.setString(2, item.getName());
			ps.setDouble(3, item.getRating());
			ps.setString(4, item.getAddress());
			ps.setString(5, item.getImageUrl());
			ps.setString(6, item.getUrl());
			ps.setDouble(7, item.getDistance());
			ps.execute();
			
			//the table name must consistent with database, or the data will not be saved to table
			sql = "INSERT IGNORE INTO categories VALUES (?, ?)";
			ps = conn.prepareStatement(sql);
			ps.setString(1, item.getItemId());
			for (String category: item.getCategories()) {
				ps.setString(2, category);
				ps.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
/*----------------------------------------------------------------------------------------------------------*/
	/*
	 *Authentication
	 */
	@Override
	public String getFullname(String userId) {
		// TODO Auto-generated method stub
		if (conn == null) {
			return "";
		}
		String name = "";
		try {
			String sql = "SELECT first_name, last_name FROM users WHERE user_id = ? ";
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, userId);
			ResultSet rs = ps.executeQuery();
			//in this project, rs has only one result, because user_id is primary key in this project
			while (rs.next()) {
				name = rs.getString("first_name") + " " + rs.getString("last_name");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return name;
	}
	

	@Override
	public boolean verifyLogin(String userId, String password) {
		// TODO Auto-generated method stub
		if (conn == null) {
			return false;
		}
		try {
			//let database to handle matching userId and password
			String sql = "SELECT * FROM users WHERE user_id = ? AND password = ? ";
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, userId);
			ps.setString(2, password);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/*----------------------------------------------------------------------------------------------------------*/
	
	/*
	 * register
	 */
	@Override
	public boolean registerUser(String userId, String password, String firstname, String lastname) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		if (conn == null) {
			System.err.println("DB Connection failed");
			return false;
		}
		try {
			//assign columns then fill in values
			String sql = "INSERT IGNORE INTO users (user_id, password, first_name, last_name) VALUES (?, ?, ?, ?)";
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, userId);
			ps.setString(2, password);
			ps.setString(3, firstname);
			ps.setString(4, lastname);
			
			return ps.executeUpdate() == 1; //return 1 -> insert return 0, userId duplicated

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
