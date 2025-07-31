package com.aspacelifetechnology.aspace_life_technology.repository;

import com.aspacelifetechnology.aspace_life_technology.models.PostModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<PostModel, Long> {
}
