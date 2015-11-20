package ar.com.example.dao;

import java.util.List;

import ar.com.example.dto.UserDTO;

public interface ExampleDAO {

  List<UserDTO> getAllUser(String language);

}
