package movieParser;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.lang.model.element.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class movieParser {

	Document doc;
	HashMap<String, Integer> mapMovie;
	HashMap<String, Integer> mapStar;

	public movieParser() {
		mapMovie = new HashMap<>();
		mapStar = new HashMap<>();
	}

	public void parseXmlFile(String file) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {

			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			// parse using builder to get DOM representation of the XML file
			doc = db.parse("./" + file);

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			se.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}

	public void insertMovies(Connection con) throws SQLException {
		try {
			org.w3c.dom.Element docEle = doc.getDocumentElement();
			NodeList dfilms = docEle.getElementsByTagName("directorfilms");

			String title = "";
			String director = "";
			int year = 2016;
			String fid = "Default";
			int movieId = 0;
			int genreId = 0;

			PreparedStatement pstmt = null;
			pstmt = con.prepareStatement("select max(id) from movies");
			ResultSet res1 = pstmt.executeQuery();

			if (res1.next()) {
				movieId = res1.getInt(1);
			}
			res1.close();

			PreparedStatement pstmt1 = null;
			pstmt1 = con.prepareStatement("select max(id) from genres");
			ResultSet res = pstmt.executeQuery();

			if (res.next()) {
				genreId = res.getInt(1);
			}
			res.close();

			PreparedStatement movieInsert = null;
			PreparedStatement genreInsert = null;
			PreparedStatement genreCheck = null;
			PreparedStatement genreMovieInsert = null;

			movieInsert = con.prepareStatement("insert into movies(id,title,year,director) values(?,?,?,?);");
			genreInsert = con.prepareStatement("insert into genres(id,name) value(?,?);");
			genreCheck = con.prepareStatement("select id from genres where name= ?;");
			genreMovieInsert = con.prepareStatement("insert into genres_in_movies(genre_id,movie_id) values (?,?);");

			if (dfilms.getLength() > 0 && dfilms != null) {
				for (int i = 0; i < dfilms.getLength(); i++) {
					org.w3c.dom.Element filmDir = (org.w3c.dom.Element) dfilms.item(i);
					NodeList films = filmDir.getElementsByTagName("films");

					if (films.getLength() > 0 && films != null) {
						for (int j = 0; j < films.getLength(); j++) {
							org.w3c.dom.Element filmElement = (org.w3c.dom.Element) films.item(j);
							NodeList filmList = filmElement.getElementsByTagName("film");

							if (filmList.getLength() > 0 && filmList != null) {
								for (int k = 0; k < filmList.getLength(); k++) {
									org.w3c.dom.Element film = (org.w3c.dom.Element) filmList.item(k);
									// id
									NodeList idList = film.getElementsByTagName("fid");
									try {
										fid = idList.item(0).getFirstChild().getNodeValue();
										fid = fid.trim();
									} catch (Exception e) {
										try {
											idList = film.getElementsByTagName("filmed");
											fid = idList.item(0).getFirstChild().getNodeValue();
										} catch (Exception e1) {
											System.out.println("Movie Id does not exist");
										}
									}
									// title
									NodeList titleList = film.getElementsByTagName("t");
									try {
										title = titleList.item(0).getFirstChild().getNodeValue();
									} catch (Exception e) {
										title = "Default";
									}
									if (title == null) {
										title = "Default";
									}

									// year
									NodeList yearList = film.getElementsByTagName("year");
									try {
										year = Integer.parseInt(yearList.item(0).getFirstChild().getNodeValue());
									} catch (Exception e) {
										year = 1992;
									}

									// director
									NodeList dirList = film.getElementsByTagName("dirs");
									try {
										org.w3c.dom.Element dir1 = (org.w3c.dom.Element) dirList.item(0);
										NodeList dir1List = dir1.getElementsByTagName("dir");
										org.w3c.dom.Element dir2 = (org.w3c.dom.Element) dir1List.item(0);
										NodeList dir2List = dir2.getElementsByTagName("dirn");
										director = dir2List.item(0).getFirstChild().getNodeValue();
									} catch (Exception e) {
										director = "Default";
									}
									if (director == null) {
										director = "Default";
									}

									movieId++;
									mapMovie.put(fid, movieId);

									title = title.trim();
									director = director.trim();

									movieInsert.setInt(1, movieId);
									movieInsert.setString(2, title);
									movieInsert.setInt(3, year);
									movieInsert.setString(4, director);
									movieInsert.addBatch();

									NodeList catList = film.getElementsByTagName("cats");
									if (catList.getLength() > 0 && catList != null) {
										org.w3c.dom.Element category = (org.w3c.dom.Element) catList.item(0);
										NodeList cat1 = category.getElementsByTagName("cat");
										if (cat1 != null) {
											for (int m = 0; m < cat1.getLength(); m++) {
												NodeList subList = cat1.item(m).getChildNodes();
												if (subList.getLength() > 0 && subList != null) {
													String genre = subList.item(0).getNodeValue();
													if (genre != null) {
														genre = genre.trim();
														genreCheck.setString(1, genre);
														ResultSet rs = genreCheck.executeQuery();

														if (rs.next()) {
															int gId = rs.getInt(1);
															genreMovieInsert.setInt(1, gId);
														} else {
															genreId++;
															genreInsert.setInt(1, genreId);
															genreInsert.setString(2, genre);
															genreInsert.executeUpdate();
															genreMovieInsert.setInt(1, genreId);
														}
														genreMovieInsert.setInt(2, movieId);
														genreMovieInsert.addBatch();
													}
												}
											}
										}
									}
								}
							}
						}

					}

				}
			}

			movieInsert.executeBatch();
			genreMovieInsert.executeBatch();
			con.commit();
			movieInsert.close();
			genreMovieInsert.close();
			genreInsert.close();
			System.out.println("success");

		} catch (Exception e) {
			System.out.println("error");
		}
	}

	public void insertStar(Connection con) throws SQLException {
		try {
			String firstName = null;
			String lastName = null;
			String stageName = null;
			String dateOfBirth = null;
			int starId = 0;

			PreparedStatement pstmt = null;
			pstmt = con.prepareStatement("select max(id) from stars;");
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				starId = rs.getInt(1);
			}
			pstmt.close();

			PreparedStatement starInsert = null;
			starInsert = con.prepareStatement("insert into stars(id,first_name,last_name,dob) values(?,?,?,?);");

			org.w3c.dom.Element docEle = doc.getDocumentElement();
			NodeList actorList = docEle.getElementsByTagName("actor");

			for (int i = 0; i < actorList.getLength(); i++) {
				firstName = null;
				lastName = null;
				stageName = null;
				dateOfBirth = null;

				org.w3c.dom.Element actor = (org.w3c.dom.Element) actorList.item(i);
				NodeList stageNameList = actor.getElementsByTagName("stagename");
				// stagename
				try {
					stageName = stageNameList.item(0).getFirstChild().getNodeValue();
					stageName = stageName.trim();
					stageName = stageName.toLowerCase();
				} catch (Exception e) {

				}

				// firstname
				try {
					NodeList fnameList = actor.getElementsByTagName("firstname");
					firstName = fnameList.item(0).getFirstChild().getNodeValue();
					firstName = firstName.trim();
				} catch (Exception e) {

				}
				// lastname
				try {
					NodeList lnameList = actor.getElementsByTagName("familyname");
					lastName = lnameList.item(0).getFirstChild().getNodeValue();
					lastName = lastName.trim();
				} catch (Exception e) {

				}

				// dob
				try {
					NodeList dobList = actor.getElementsByTagName("dob");
					dateOfBirth = dobList.item(0).getFirstChild().getNodeValue();
				} catch (Exception e) {

				}
				if (firstName == null) {
					firstName = "Default";
				}

				if (lastName == null) {
					lastName = firstName;
					firstName = "";
				}

				if (dateOfBirth == null) {
					dateOfBirth = "1992-08-25";
				} else {
					dateOfBirth = dateOfBirth + "-08-25";
				}
				try {
					starInsert.setDate(4, java.sql.Date.valueOf(dateOfBirth));
				} catch (Exception e) {
					dateOfBirth = "1992-08-25";
					starInsert.setDate(4, java.sql.Date.valueOf(dateOfBirth));
				}

				starId++;

				starInsert.setInt(1, starId);
				starInsert.setString(2, firstName);
				starInsert.setString(3, lastName);
				starInsert.addBatch();

				if (stageName != null) {
					mapStar.put(stageName, starId);
				}

			}
			starInsert.executeBatch();
			con.commit();
			starInsert.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		System.out.println("Success 2...");
	}

	public void insertStarInMovies(Connection con) throws SQLException {
		try {

			int movieId;
			int starId;
			String filmId = null;
			String stageName = null;
			int check = 0;

			PreparedStatement insertStarMovie = null;
			insertStarMovie = con.prepareStatement("insert into stars_in_movies(star_id,movie_id) values(?,?);");

			org.w3c.dom.Element docEle = doc.getDocumentElement();
			NodeList dirList = docEle.getElementsByTagName("dirfilms");

			for (int i = 0; i < dirList.getLength(); i++) {
				org.w3c.dom.Element dir1 = (org.w3c.dom.Element) dirList.item(i);
				NodeList filmList = dir1.getElementsByTagName("filmc");

				if (filmList != null) {
					for (int j = 0; j < filmList.getLength(); j++) {
						org.w3c.dom.Element dir2 = (org.w3c.dom.Element) filmList.item(j);
						NodeList list = dir2.getElementsByTagName("m");
						if (list != null) {
							for (int k = 0; k < list.getLength(); k++) {
								filmId = null;
								stageName = null;
								check = 0;

								org.w3c.dom.Element ele1 = (org.w3c.dom.Element) list.item(k);
								NodeList fList = ele1.getElementsByTagName("f");
								if (fList.getLength() > 0 && fList != null) {
									try {
										filmId = fList.item(0).getFirstChild().getNodeValue();
										filmId = filmId.trim();
									} catch (Exception e) {

									}
								}

								NodeList aList = ele1.getElementsByTagName("a");
								if (aList != null) {
									try {
										stageName = aList.item(0).getFirstChild().getNodeValue();
										stageName = stageName.trim();
										stageName = stageName.toLowerCase();
									} catch (Exception e) {

									}
								}

								if (filmId != null && stageName != null) {
									if (!mapMovie.containsKey(filmId))
										check = 1;
									if (!mapStar.containsKey(stageName))
										check = 1;

									if (check == 0) {
										starId = mapStar.get(stageName);
										movieId = mapMovie.get(filmId);

										insertStarMovie.setInt(1, starId);
										insertStarMovie.setInt(2, movieId);
										insertStarMovie.addBatch();
									}
								}
							}
						}
					}
				}
			}

			insertStarMovie.executeBatch();
			con.commit();
			insertStarMovie.close();
			System.out.println("success 3...");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// TODO Auto-generated method stub

		Connection con = null;

		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql:///moviedb", "testuser", "testpass");

			System.out.println("Successfully Logged In");
		} catch (com.mysql.jdbc.exceptions.MySQLSyntaxErrorException e) {
			System.out.println("Database not found...");
		} catch (Exception e) {
			System.out.println("Login Failed....Try again..");
		}

		con.setAutoCommit(false);
		movieParser par = new movieParser();

		long start= System.currentTimeMillis();

		par.parseXmlFile("mains243.xml");
		System.out.println("parsed mains");
		par.insertMovies(con);
		
		long stop1= System.currentTimeMillis();


		par.parseXmlFile("actors63.xml");
		System.out.println("parsed actors");
		par.insertStar(con);

		long stop2= System.currentTimeMillis();

		
		par.parseXmlFile("casts124.xml");
		System.out.println("parsed casts");
		par.insertStarInMovies(con);
		
		long stop3= System.currentTimeMillis();

		System.out.println("Mains:"+ (stop1-start));
		System.out.println("Actors:"+ (stop2-stop1));
		System.out.println("Casts:"+ (stop3-stop2));


	}

}
