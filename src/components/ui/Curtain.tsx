import type { ReactNode } from "react";
import type { PhotoKey } from "@/content/photos";
import { PhotoFill } from "./PhotoFill";

type Props = {
  photo: PhotoKey;
  sizes: string;
  /** Which edge the photo is uncovered from. */
  from?: "up" | "left" | "right";
  /** Uncover on page load instead of on scroll. Only for a photo that starts in view. */
  onLoad?: boolean;
  priority?: boolean;
  position?: string;
  /** Sizing and placement for the frame, such as an aspect ratio or a height. */
  className?: string;
  /** Anything laid over the photo, like a shade or a caption. It sits inside the frame. */
  children?: ReactNode;
};

const direction = { up: "", left: "from-left", right: "from-right" } as const;

/**
 * A large photo that gets uncovered as it scrolls in, while the image inside eases back
 * from a slight zoom. The motion lives in globals.css; with reduced motion, or in a
 * browser without scroll timelines, the photo simply sits in place.
 */
export function Curtain({ photo, sizes, from = "up", onLoad = false, priority = false, position, className = "", children }: Props) {
  return (
    <div className={`${onLoad ? "curtain-load" : "curtain"} ${direction[from]} relative overflow-hidden bg-bg ${className}`}>
      <div className="curtain-frame absolute inset-0 overflow-hidden">
        <div className="curtain-photo absolute inset-0">
          <PhotoFill photo={photo} sizes={sizes} priority={priority} position={position} motion={onLoad ? "drift" : "settle"} />
        </div>
      </div>
      {children}
    </div>
  );
}
