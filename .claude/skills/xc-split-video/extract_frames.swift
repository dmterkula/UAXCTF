// Extract frames from a video (handles iPhone rotation) using AVFoundation; no ffmpeg needed.
// Build once:  swiftc -O extract_frames.swift -o <scratch>/extract_frames
// Usage:       extract_frames <video> <outdir> <step_sec> [start_sec] [end_sec] [max_px]
// Writes <outdir>/f_<seconds>.jpg (e.g. f_0094.50.jpg) and prints duration/size/fps.
import AVFoundation
import AppKit

let a = CommandLine.arguments
guard a.count >= 4 else {
    print("usage: extract_frames <video> <outdir> <step_sec> [start] [end] [max_px]"); exit(1)
}
let asset = AVURLAsset(url: URL(fileURLWithPath: a[1]))
let out = a[2]
let step = Double(a[3])!
let dur = CMTimeGetSeconds(asset.duration)
let start = a.count > 4 ? Double(a[4])! : 0
let end = a.count > 5 ? min(Double(a[5])!, dur) : dur
let maxPx = a.count > 6 ? Double(a[6])! : 960
try? FileManager.default.createDirectory(atPath: out, withIntermediateDirectories: true)
print("duration \(dur)")
if let t = asset.tracks(withMediaType: .video).first {
    print("size \(t.naturalSize) fps \(t.nominalFrameRate)")
}
let gen = AVAssetImageGenerator(asset: asset)
gen.appliesPreferredTrackTransform = true
gen.requestedTimeToleranceBefore = .zero
gen.requestedTimeToleranceAfter = .zero
gen.maximumSize = CGSize(width: maxPx, height: maxPx)
var t = start
while t <= end + 1e-6 {
    if let img = try? gen.copyCGImage(at: CMTime(seconds: t, preferredTimescale: 600), actualTime: nil) {
        let data = NSBitmapImageRep(cgImage: img).representation(using: .jpeg, properties: [.compressionFactor: 0.85])!
        try! data.write(to: URL(fileURLWithPath: String(format: "%@/f_%07.2f.jpg", out, t)))
    }
    t += step
}
