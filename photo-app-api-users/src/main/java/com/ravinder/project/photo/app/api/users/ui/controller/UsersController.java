package com.ravinder.project.photo.app.api.users.ui.controller;

import com.ravinder.project.photo.app.api.users.service.UsersService;
import com.ravinder.project.photo.app.api.users.shared.UserDto;
import com.ravinder.project.photo.app.api.users.ui.model.CreateUserRequestModel;
import com.ravinder.project.photo.app.api.users.ui.model.CreateUserResponseModel;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UsersController {

    private Environment env;
    private UsersService usersService;
    private ModelMapper modelMapper;

    public UsersController(Environment env, UsersService usersService, ModelMapper modelMapper) {
        this.env = env;
        this.usersService = usersService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/status/check")
    public String status(){
        return "Working on port " + env.getProperty("local.server.port");
    }

    @PostMapping
    public ResponseEntity<CreateUserResponseModel> createUser(@Valid @RequestBody CreateUserRequestModel userRequestModel){
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserDto userDto = modelMapper.map(userRequestModel, UserDto.class);
        UserDto createdUser = usersService.createUser(userDto);
        CreateUserResponseModel responseBody = modelMapper.map(createdUser, CreateUserResponseModel.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }
}
