package vn.fpt.seima.seimaserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.BankInformation;

import java.util.Optional;

@Repository
public interface BankInformationRepository extends JpaRepository<BankInformation, Integer> {

} 