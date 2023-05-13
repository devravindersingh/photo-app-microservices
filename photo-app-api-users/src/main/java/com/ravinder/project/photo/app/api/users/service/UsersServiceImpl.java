package com.ravinder.project.photo.app.api.users.service;

import com.ravinder.project.photo.app.api.users.data.UserEntity;
import com.ravinder.project.photo.app.api.users.data.UserRepository;
import com.ravinder.project.photo.app.api.users.shared.UserDto;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UsersServiceImpl implements UsersService{

    private ModelMapper modelMapper;
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    private UserRepository userRepository;

    public UsersServiceImpl(ModelMapper modelMapper, BCryptPasswordEncoder bCryptPasswordEncoder, UserRepository userRepository) {
        this.modelMapper = modelMapper;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.userRepository = userRepository;
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        userDto.setUserId(UUID.randomUUID().toString());
        userDto.setEncryptedPassword(
                bCryptPasswordEncoder.encode(userDto.getPassword()));
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserEntity userEntity = modelMapper.map(userDto, UserEntity.class);

        UserEntity createdUser = userRepository.save(userEntity);
        return modelMapper.map(createdUser, UserDto.class);
    }
}
