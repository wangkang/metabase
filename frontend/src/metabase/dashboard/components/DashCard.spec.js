import React from "react";
import renderer from "react-test-renderer";
import { render } from "enzyme";
import { assocIn } from "icepick";

import DashCard from "./DashCard";

jest.mock("metabase/visualizations/components/Visualization.jsx");

const DEFAULT_PROPS = {
    dashcard: {
        card: { id: 1 },
        series: [],
        parameter_mappings: []
    },
    dashcardData: {
        1: { cols: [], rows: [] }
    },
    cardDurations: {},
    parameterValues: {},
    markNewCardSeen: () => {},
    fetchCardData: () => {}
};

describe("DashCard", () => {
    it("should render with no special classNames", () => {
        expect(
            renderer.create(<DashCard {...DEFAULT_PROPS} />).toJSON()
        ).toMatchSnapshot();
    });
    it("should render unmapped card with Card--unmapped className", () => {
        const props = assocIn(DEFAULT_PROPS, ["parameterValues", "foo"], "bar");
        const dashCard = render(<DashCard {...props} />);
        expect(dashCard.find(".Card--recent")).toHaveLength(0);
        expect(dashCard.find(".Card--unmapped")).toHaveLength(1);
        expect(dashCard.find(".Card--slow")).toHaveLength(0);
    });
    it("should render slow card with Card--slow className", () => {
        const props = assocIn(DEFAULT_PROPS, ["cardDurations", 1], {
            average: 1,
            fast_threshold: 1
        });
        const dashCard = render(<DashCard {...props} />);
        expect(dashCard.find(".Card--recent")).toHaveLength(0);
        expect(dashCard.find(".Card--unmapped")).toHaveLength(0);
        expect(dashCard.find(".Card--slow")).toHaveLength(1);
    });
    it("should render new card with Card--recent className", () => {
        const props = assocIn(DEFAULT_PROPS, ["dashcard", "isAdded"], true);
        const dashCard = render(<DashCard {...props} />);
        expect(dashCard.find(".Card--recent")).toHaveLength(1);
        expect(dashCard.find(".Card--unmapped")).toHaveLength(0);
        expect(dashCard.find(".Card--slow")).toHaveLength(0);
    });
});
