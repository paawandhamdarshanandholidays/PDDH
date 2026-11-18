import { getYouTubeVideoId } from './gallery-video-url';

describe('getYouTubeVideoId', () => {
  it('recognizes watch, short, and shorts links', () => {
    expect(getYouTubeVideoId('https://www.youtube.com/watch?v=aqz-KE-bpKQ')).toBe('aqz-KE-bpKQ');
    expect(getYouTubeVideoId('https://youtu.be/aqz-KE-bpKQ?t=10')).toBe('aqz-KE-bpKQ');
    expect(getYouTubeVideoId('https://m.youtube.com/shorts/aqz-KE-bpKQ')).toBe('aqz-KE-bpKQ');
  });

  it('rejects unrelated domains and malformed video identifiers', () => {
    expect(getYouTubeVideoId('https://example.test/watch?v=aqz-KE-bpKQ')).toBeNull();
    expect(getYouTubeVideoId('https://youtube.com/watch?v=short')).toBeNull();
  });
});
