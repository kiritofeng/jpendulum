# jpendulum

Tracks a pendulum, given a video as input, and returns a `.csv` with angular displacement of the pendulum.

## Development

Requires: Git, Maven

1. Clone the repository. `git clone https://github.com/kiritofeng/jpendulum`
2. Switch to directory. `cd jpendulum`
3. Build with Maven. `mvn clean compile assembly:single`

## Usage

After obtaining the compiled `jar`, run `java -jar jpendulum.jar <video> <output file> <colour> <tolerance> <enable gui?>`.

- `video` is the video file to analyse.
- `output file` is the file to output the result to
- `colour` is the colour to track
- `tolerance` is how much tolerance to give
- `enable gui` is an optional argument that defaults to false. By setting it to true, you can see a playback of the video, where the tracked regions are highlighted.
