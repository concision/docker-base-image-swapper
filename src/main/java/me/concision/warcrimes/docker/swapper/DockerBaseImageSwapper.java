package me.concision.warcrimes.docker.swapper;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DockerBaseImageSwapper {
    @NonNull
    @Getter
    private final CommandArguments args;

    public void execute() {

    }
}
