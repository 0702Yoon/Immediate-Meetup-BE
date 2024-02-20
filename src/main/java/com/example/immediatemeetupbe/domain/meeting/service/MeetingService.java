package com.example.immediatemeetupbe.domain.meeting.service;

import com.example.immediatemeetupbe.domain.meeting.dto.MeetingDto;
import com.example.immediatemeetupbe.domain.meeting.dto.request.MeetingModifyRequest;
import com.example.immediatemeetupbe.domain.meeting.dto.request.MeetingRegisterRequest;
import com.example.immediatemeetupbe.domain.meeting.dto.response.MeetingListResponse;
import com.example.immediatemeetupbe.domain.meeting.dto.response.MeetingResponse;
import com.example.immediatemeetupbe.domain.meeting.entity.Meeting;
import com.example.immediatemeetupbe.domain.meeting.repository.MeetingRepository;
import com.example.immediatemeetupbe.domain.member.repository.MemberRepository;
import com.example.immediatemeetupbe.domain.participant.entity.Participant;
import com.example.immediatemeetupbe.domain.member.entity.Member;
import com.example.immediatemeetupbe.domain.participant.entity.host.Role;
import com.example.immediatemeetupbe.global.exception.BaseException;
import com.example.immediatemeetupbe.global.jwt.AuthUtil;

import com.example.immediatemeetupbe.domain.participant.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.immediatemeetupbe.global.exception.BaseExceptionStatus.*;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final MemberRepository memberRepository;
    private final AuthUtil authUtil;

    @Transactional
    public void register(MeetingRegisterRequest meetingRegisterRequest) {
        // 방을 만든 사람
        Member member = authUtil.getLoginMember();
        Meeting meeting = meetingRepository.save(meetingRegisterRequest.toEntity());

        participantRepository.save(Participant.builder()
                .meeting(meeting)
                .member(member)
                .role(Role.HOST)
                .build());
    }

    @Transactional
    public void modify(MeetingModifyRequest meetingModifyRequest) {
        if (meetingRepository.existsById(meetingModifyRequest.getId())) {
            throw new BaseException(NO_EXIST_MEETING.getMessage());
        }
        Meeting meeting = meetingRepository.getById(meetingModifyRequest.getId());
        meeting.update(meetingModifyRequest.getTitle(), meetingModifyRequest.getContent(),
                meetingModifyRequest.getFirstDay(), meetingModifyRequest.getLastDay());
    }

    @Transactional
    public MeetingResponse getMeetingInfoById(Long id) {
        if (meetingRepository.existsById(id)) {
            throw new BaseException(NO_EXIST_MEETING.getMessage());
        }
        Meeting meeting = meetingRepository.getById(id);
        return MeetingResponse.from(meeting);
    }

    @Transactional
    public Meeting getMeetingInfo(Long id) {
        if (meetingRepository.existsById(id)) {
            throw new BaseException(NO_EXIST_MEETING.getMessage());
        }
        return meetingRepository.getById(id);
    }

    @Transactional
    public void delete(Long id) {
        if (meetingRepository.existsById(id)) {
            throw new BaseException(NO_EXIST_MEETING.getMessage());
        }

        Meeting meeting = meetingRepository.getById(id);
        meetingRepository.delete(meeting);
    }

    @Transactional(readOnly = true)
    public MeetingListResponse getAllMeetings() {
        Member member = authUtil.getLoginMember();
        List<Participant> participantList = member.getParticipantList();

        List<MeetingDto> meetingDtoList = participantList.stream()
                .map(meetingMember -> MeetingDto.from(meetingMember.getMeeting()))
                .toList();

        return MeetingListResponse.builder()
                .meetings(meetingDtoList)
                .build();
    }

    @Transactional
    public void inviteMember(Long meetingId, Long memberId) {
        Member host = authUtil.getLoginMember();
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BaseException(NO_EXIST_MEETING.getMessage()));
        Participant participant = participantRepository.findByMemberAndMeeting(host, meeting)
                .orElseThrow(() -> new BaseException(NO_EXIST_PARTICIPANT.getMessage()));

        if (participant.getRole() != Role.HOST) {
            throw new BaseException(NOT_HOST_OF_MEETING.getMessage());
        }

        Member invitedMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new BaseException(NO_EXIST_MEMBER.getMessage()));

        if (participantRepository.existsByMemberAndMeeting(invitedMember, meeting)) {
            throw new BaseException(ALREADY_INVITED.getMessage());
        }

        participantRepository.save(Participant.builder()
                .meeting(meeting)
                .member(invitedMember)
                .role(Role.MEMBER)
                .build());
    }
}