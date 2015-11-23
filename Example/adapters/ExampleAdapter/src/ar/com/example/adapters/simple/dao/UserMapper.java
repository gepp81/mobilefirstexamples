package ar.com.example.adapters.simple.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import ar.com.example.adapters.simple.dto.UserDTO;

public class UserMapper implements RowMapper<UserDTO> {

  private static final String LASTNAME = "lastname";
  private static final String LANGUAGE = "language";
  private static final String FIRSTNAME = "firstname";

  @Override
  public UserDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
    UserDTO dto = new UserDTO();
    dto.setFirstName(rs.getString(FIRSTNAME));
    dto.setLastName(rs.getString(LASTNAME));
    dto.setLanguage(rs.getString(LANGUAGE));
    return dto;
  }

}