package ar.com.example.adapters.simple.dao;

import java.util.List;

import ar.com.example.adapters.simple.dto.UserDTO;

public interface ExampleDAO {

  List<UserDTO> getAllUser(String language);

}
