package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.entity.Rank;
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.repository.RankRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RankService {

    @Autowired
    private RankRepository rankRepository;

    public void saveRank(Rank rank) {
        rankRepository.save(rank);
    }

    public void createDefaultRank(users user) {
        Rank defaultRank = new Rank();
        defaultRank.setUser(user);
        defaultRank.setRank("Chưa có"); // Default rank
        defaultRank.setPoints(0); // Points for voucher redemption
        rankRepository.save(defaultRank);
    }


}