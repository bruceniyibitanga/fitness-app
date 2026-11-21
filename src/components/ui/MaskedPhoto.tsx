import type { PhotoKey } from "@/content/photos";
import { PhotoFill } from "./PhotoFill";

type Props = {
  photo: PhotoKey;
  /** Tells the browser how wide the photo shows, so it downloads the right size. */
  sizes: string;
  /** Sizing for the frame, such as an aspect ratio or a height. */
  frame: string;
  /** Open on page load instead of on scroll. Only for a photo that starts in view. */
  onLoad?: boolean;
  priority?: boolean;
  position?: string;
  /** A short caption under the photo, set small, like a magazine. */
  caption?: string;
  className?: string;
};

/**
 * A photo that opens out of a slightly smaller frame as it arrives, while the image eases
 * back from a small zoom. The motion lives in globals.css; with reduced motion, or in a
 * browser without scroll timelines, the photo simply sits in place.
 */
export function MaskedPhoto({ photo, sizes, frame, onLoad = false, priority = false, position, caption, className = "" }: Props) {
  return (
    <figure className={className}>
      <div className={`${onLoad ? "mask-load" : "mask"} relative overflow-hidden bg-surface ${frame}`}>
        <PhotoFill photo={photo} sizes={sizes} priority={priority} position={position} motion={onLoad ? "drift" : "none"} />
      </div>
      {caption ? <figcaption className="mono mt-3 text-[10px] text-muted">{caption}</figcaption> : null}
    </figure>
  );
}
