package engine.controller;

import engine.exception.ResourceNotFoundException;
import engine.model.*;
import engine.repository.QuizRepository;
import engine.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;

import java.util.List;

@RestController
public class QuizController {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public QuizController() {
    }

    @GetMapping(path = "/api/quizzes")
    public List<Quiz> getQuiz() {
        return quizRepository.findAll();
    }

    @GetMapping(path = "/api/quizzes/{id}")
    public ResponseEntity<Quiz> getQuiz(@PathVariable long id) throws ResourceNotFoundException {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found for this id : "+id));
        return ResponseEntity.ok().body(quiz);
    }

    @PostMapping(path = "/api/quizzes")
    public Quiz getQuiz(@Valid @RequestBody Quiz quiz) {

        quizRepository.save(quiz);
        return quiz;
    }

    @PostMapping(path = "/api/quizzes/{id}/solve", consumes = "application/json")
    public Feedback resolveQuiz(@PathVariable int id, @RequestBody Answer answer) throws ResourceNotFoundException {
        Quiz quiz = quizRepository.findById((long)id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found for this id : "+id));
        if(quiz.getAnswer() != null){
            Arrays.sort(quiz.getAnswer());
        } else if(answer.getAnswer() != null){
            Arrays.sort(answer.getAnswer());
        }
        boolean success;
        if(quiz.getAnswer() == null && Arrays.equals(answer.getAnswer(), new int[0])){
            success = true;
        } else {
            success = Arrays.equals(quiz.getAnswer(), answer.getAnswer());
        }
        String feedback = success ? Message.CONGRATULATIONS : Message.WRONG_ANSWER;
        return new Feedback(success, feedback);
    }

    @PostMapping(path = "/api/register")
    public ResponseEntity register(@Valid @RequestBody User user){

        List<User> users = userRepository.findAll();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles("ROLE_USER");
        user.setActive(true);
        if(users.isEmpty()){
            userRepository.save(user);
            return new ResponseEntity("", HttpStatus.OK);
        } else if(users.stream().anyMatch(x -> x.getEmail().equals(user.getEmail()))) {
            return new ResponseEntity("", HttpStatus.BAD_REQUEST);
        } else {
            userRepository.save(user);
            return new ResponseEntity("", HttpStatus.OK);
        }
    }

    @DeleteMapping(path = "/api/quizzes/{id}")
    public HttpStatus deleteQuiz(@PathVariable long id, Authentication authentication) throws ResourceNotFoundException {

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found for this id : "+id));
        User user = userRepository.findById(quiz.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found for this id : "+quiz.getUser().getId()));
        if(((User)authentication.getPrincipal()).getEmail() == user.getEmail()){
            quizRepository.delete(quiz);
            return HttpStatus.NO_CONTENT;
        } else {
            return HttpStatus.FORBIDDEN;
        }
    }
}