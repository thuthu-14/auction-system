package server.storage;

import java.sql.Connection;
import java.sql.Statement;

public class SchemaInitializer {

    public static void init() {
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement()) {

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                  user_id TEXT PRIMARY KEY,
                  username TEXT UNIQUE NOT NULL,
                  password TEXT NOT NULL,
                  email TEXT UNIQUE NOT NULL,
                  role TEXT NOT NULL,
                  wallet REAL DEFAULT 0,
                  active INTEGER DEFAULT 1,
                  created_at INTEGER,
                  is_seller INTEGER DEFAULT 0,
                  shop_name TEXT,
                  shop_phone TEXT,
                  shop_address TEXT,
                  shop_email TEXT
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS items (
                  item_id TEXT PRIMARY KEY,
                  type TEXT NOT NULL,
                  name TEXT NOT NULL,
                  description TEXT,
                  starting_price REAL NOT NULL,
                  category TEXT NOT NULL,
                  seller_id TEXT,
                  created_at INTEGER,

                  brand TEXT,
                  warranty_period TEXT,
                  model TEXT,
                  odometer INTEGER,
                  material TEXT,
                  weight REAL,
                  creator TEXT
                )
            """);


            st.executeUpdate("""
    CREATE TABLE IF NOT EXISTS item_images (
      image_id TEXT PRIMARY KEY,
      item_id TEXT NOT NULL,
      image_data BLOB NOT NULL,
      image_size INTEGER,
      image_type TEXT DEFAULT 'jpg',
      sort_index INTEGER DEFAULT 0,
      created_at INTEGER,
      FOREIGN KEY (item_id) REFERENCES items(item_id)
    )
""");



            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS auctions (
                  auction_id TEXT PRIMARY KEY,
                  item_id TEXT NOT NULL,
                  seller_id TEXT NOT NULL,
                  seller_name TEXT NOT NULL,
                  current_price REAL,
                  highest_bidder_id TEXT,
                  highest_bidder_name TEXT,
                  status TEXT,
                  start_time INTEGER,
                  end_time INTEGER,
                  created_at INTEGER,
                  reserve_price REAL,
                  minimum_bid_increment REAL,
                  view_count INTEGER DEFAULT 0
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bids (
                  bid_id TEXT PRIMARY KEY,
                  auction_id TEXT NOT NULL,
                  bidder_id TEXT NOT NULL,
                  bidder_name TEXT NOT NULL,
                  amount REAL NOT NULL,
                  bid_time INTEGER,
                  status TEXT
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS notifications (
                  notification_id TEXT PRIMARY KEY,
                  user_id TEXT NOT NULL,
                  type TEXT,
                  title TEXT,
                  description TEXT,
                  created_at INTEGER,
                  time_ago TEXT,
                  button_text TEXT,
                  reference_id TEXT,
                  is_read INTEGER DEFAULT 0
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transactions (
                  id TEXT PRIMARY KEY,
                  user_id TEXT NOT NULL,
                  type TEXT NOT NULL,
                  amount REAL NOT NULL,
                  balance_after REAL NOT NULL,
                  description TEXT,
                  timestamp INTEGER
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bank_accounts (
                  user_id TEXT PRIMARY KEY,
                  username TEXT,
                  email TEXT,
                  bank_name TEXT,
                  account_number TEXT,
                  initial_balance REAL,
                  created_at INTEGER
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS user_owned_auctions (
                  user_id TEXT NOT NULL,
                  auction_id TEXT NOT NULL,
                  PRIMARY KEY (user_id, auction_id)
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS user_bids (
                  user_id TEXT NOT NULL,
                  bid_id TEXT NOT NULL,
                  PRIMARY KEY (user_id, bid_id)
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS auction_bids (
                  auction_id TEXT NOT NULL,
                  bid_id TEXT NOT NULL,
                  PRIMARY KEY (auction_id, bid_id)
                )
            """);

        } catch (Exception e) {
            throw new RuntimeException("Schema init failed", e);
        }
    }
}