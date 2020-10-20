package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.exception.RequestNotValidException;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RsService {
  final RsEventRepository rsEventRepository;
  final UserRepository userRepository;
  final VoteRepository voteRepository;
  final TradeRepository tradeRepository;

  public RsService(RsEventRepository rsEventRepository, UserRepository userRepository, VoteRepository voteRepository, TradeRepository tradeRepository) {
    this.rsEventRepository = rsEventRepository;
    this.userRepository = userRepository;
    this.voteRepository = voteRepository;
    this.tradeRepository = tradeRepository;
  }

  public void vote(Vote vote, int rsEventId) {
    Optional<RsEventDto> rsEventDto = rsEventRepository.findById(rsEventId);
    Optional<UserDto> userDto = userRepository.findById(vote.getUserId());
    if (!rsEventDto.isPresent()
        || !userDto.isPresent()
        || vote.getVoteNum() > userDto.get().getVoteNum()) {
      throw new RuntimeException();
    }
    VoteDto voteDto =
        VoteDto.builder()
            .localDateTime(vote.getTime())
            .num(vote.getVoteNum())
            .rsEvent(rsEventDto.get())
            .user(userDto.get())
            .build();
    voteRepository.save(voteDto);
    UserDto user = userDto.get();
    user.setVoteNum(user.getVoteNum() - vote.getVoteNum());
    userRepository.save(user);
    RsEventDto rsEvent = rsEventDto.get();
    rsEvent.setVoteNum(rsEvent.getVoteNum() + vote.getVoteNum());
    rsEventRepository.save(rsEvent);
  }

  public void buy(Trade trade, int id) {
    Optional<RsEventDto> optionalRsEventDto = rsEventRepository.findById(id);
    if (!optionalRsEventDto.isPresent()) {
      throw new RequestNotValidException("invalid event id");
    }
    RsEventDto rsEventDto = optionalRsEventDto.get();
    int rank = trade.getRank();
    Optional<TradeDto> prevTrade = tradeRepository.findById(rank);
    if (!prevTrade.isPresent()) {
      freshBuy(trade, optionalRsEventDto.get());
      return;
    }
    TradeDto tradeDto = prevTrade.get();
    if (trade.getAmount() < tradeDto.getAmount()) {
      throw new RequestNotValidException("trade amount not enough");
    }
    RsEventDto originRsEventDto = tradeDto.getRsEventDto();
    RsEventDto newRsEventDto = optionalRsEventDto.get();

    tradeDto.setRsEventDto(newRsEventDto);
    newRsEventDto.setTradeDto(tradeDto);

    originRsEventDto.setTradeDto(null);
    rsEventRepository.deleteById(originRsEventDto.getId());

    tradeDto.setAmount(trade.getAmount());
    tradeRepository.save(tradeDto);
  }

  private void freshBuy(Trade trade, RsEventDto eventDto) {
    TradeDto tradeDto = TradeDto.builder()
            .amount(trade.getAmount())
            .rank(trade.getRank())
            .rsEventDto(eventDto)
            .build();
    tradeRepository.save(tradeDto);

    eventDto.setTradeDto(tradeDto);
    rsEventRepository.save(eventDto);
  }
}
