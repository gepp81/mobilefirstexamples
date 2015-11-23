package ar.com.example.adapters.simple.dao;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import ar.com.example.adapters.simple.dto.UserDTO;

public class ExampleDAOImpl implements ExampleDAO {

  private JdbcTemplate jdbcTemplate;

  public ExampleDAOImpl(final DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Override
  public List<UserDTO> getAllUser(final String language) {
    Object[] objs = { language };
    String query = "SELECT * FROM user WHERE language = ?";
    List<UserDTO> users = jdbcTemplate.query(query, objs, new UserMapper());
    return users;

  }

}
